// Firebase configuration
const firebaseConfig = {
    apiKey: "AIzaSyB0mjMEG4whE15eaFq73yXi21GouoOTTnI",
    authDomain: "litera-a760b.firebaseapp.com",
    projectId: "litera-a760b",
    storageBucket: "litera-a760b.appspot.com",
    messagingSenderId: "1475002019552",
    appId: "1:471537311953:android:79f78feb8516e8a661e5f0"
};

// Initialize Firebase
firebase.initializeApp(firebaseConfig);
const db = firebase.firestore();
const auth = firebase.auth();

// Global variables
let authorsList = [];
let currentEditMode = false;
let bookModalBS, authorModalBS, userModalBS, deleteModalBS, viewBookModalBS, viewUserModalBS, viewAuthorModalBS;

// DOM Elements
const loginSection = document.getElementById('loginSection');
const adminDashboard = document.getElementById('adminDashboard');
const userEmailSpan = document.getElementById('userEmail');

// Initialize Bootstrap modals
document.addEventListener('DOMContentLoaded', function() {
    bookModalBS = new bootstrap.Modal(document.getElementById('addBookModal'));
    authorModalBS = new bootstrap.Modal(document.getElementById('addAuthorModal'));
    userModalBS = new bootstrap.Modal(document.getElementById('addUserModal'));
    deleteModalBS = new bootstrap.Modal(document.getElementById('deleteConfirmModal'));
    viewBookModalBS = new bootstrap.Modal(document.getElementById('viewBookModal'));
    viewUserModalBS = new bootstrap.Modal(document.getElementById('viewUserModal'));
    viewAuthorModalBS = new bootstrap.Modal(document.getElementById('viewAuthorModal'));
    
    // Hide the "Add New User" button
    if (document.querySelector('button[data-bs-target="#addUserModal"]')) {
        document.querySelector('button[data-bs-target="#addUserModal"]').style.display = 'none';
    }
    
    // Set up event listeners
    setupEventListeners();
    
    // Check authentication state
    checkAuthState();
});

// Check if user is authenticated
function checkAuthState() {
    auth.onAuthStateChanged(user => {
        if (user) {
            // Check if user is admin
            db.collection('users').doc(user.uid).get().then(doc => {
                if (doc.exists && doc.data().role === 'admin') {
                    // Show admin dashboard
                    loginSection.classList.add('hidden');
                    adminDashboard.classList.remove('hidden');
                    userEmailSpan.textContent = user.email;
                    
                    // Load data
                    loadAuthors();
                    loadBooks();
                    loadUsers();
                } else {
                    // Not an admin, log them out
                    auth.signOut().then(() => {
                        alert('You do not have admin privileges.');
                        loginSection.classList.remove('hidden');
                        adminDashboard.classList.add('hidden');
                    });
                }
            }).catch(error => {
                console.error("Error checking user role:", error);
                alert("Error checking user role: " + error.message);
                auth.signOut();
            });
        } else {
            // Not logged in
            loginSection.classList.remove('hidden');
            adminDashboard.classList.add('hidden');
        }
    });
}

// Setup event listeners
function setupEventListeners() {
    // Login
    document.getElementById('loginBtn').addEventListener('click', handleLogin);
    
    // Logout
    document.getElementById('logoutBtn').addEventListener('click', () => {
        auth.signOut();
    });
    
    // Books
    document.getElementById('saveBookBtn').addEventListener('click', saveBook);
    
    // Authors
    document.getElementById('saveAuthorBtn').addEventListener('click', saveAuthor);
    
    // Users
    document.getElementById('saveUserBtn').addEventListener('click', saveUser);
    
    // Delete confirmation
    document.getElementById('confirmDeleteBtn').addEventListener('click', confirmDelete);
    
    // Preview images
    document.getElementById('bookCover').addEventListener('blur', function() {
        previewImage('bookCover', 'coverPreview');
    });
    
    document.getElementById('authorPfp').addEventListener('blur', function() {
        previewImage('authorPfp', 'profilePicPreview');
    });
    
    // Modal resets
    document.getElementById('addBookModal').addEventListener('hidden.bs.modal', () => {
        document.getElementById('addBookForm').reset();
        document.getElementById('bookId').value = '';
        document.getElementById('addBookModalLabel').textContent = 'Add New Book';
        document.getElementById('coverPreview').innerHTML = '';
        currentEditMode = false;
    });
    
    document.getElementById('addAuthorModal').addEventListener('hidden.bs.modal', () => {
        document.getElementById('addAuthorForm').reset();
        document.getElementById('authorId').value = '';
        document.getElementById('addAuthorModalLabel').textContent = 'Add New Author';
        document.getElementById('profilePicPreview').innerHTML = '';
        currentEditMode = false;
    });
    
    document.getElementById('addUserModal').addEventListener('hidden.bs.modal', () => {
        document.getElementById('addUserForm').reset();
        document.getElementById('userId').value = '';
        
        // Show all form fields for future use
        const form = document.getElementById('addUserForm');
        for (let i = 0; i < form.elements.length; i++) {
            const element = form.elements[i];
            if (element.id !== 'userPassword') {
                const elementLabel = document.querySelector(`label[for="${element.id}"]`);
                if (elementLabel) elementLabel.style.display = '';
                element.style.display = '';
            }
        }
        
        // But still hide password field
        const passwordField = document.getElementById('userPassword');
        const passwordLabel = document.querySelector('label[for="userPassword"]');
        if (passwordField) passwordField.style.display = 'none';
        if (passwordLabel) passwordLabel.style.display = 'none';
        
        // Reset readOnly on email field
        document.getElementById('userEmail').readOnly = false;
        
        document.getElementById('addUserModalLabel').textContent = 'Edit User';
        currentEditMode = false;
    });
}

// Preview image function
function previewImage(inputId, previewId) {
    const url = document.getElementById(inputId).value;
    const previewDiv = document.getElementById(previewId);
    
    if (url) {
        previewDiv.innerHTML = `<img src="${url}" alt="Preview" class="preview-image">`;
    } else {
        previewDiv.innerHTML = '';
    }
}

// Login handler
function handleLogin() {
    const email = document.getElementById('email').value.trim();
    const password = document.getElementById('password').value;
    const loginError = document.getElementById('loginError');
    
    if (!email || !password) {
        loginError.textContent = "Please enter both email and password";
        return;
    }
    
    loginError.textContent = "Logging in...";
    
    auth.signInWithEmailAndPassword(email, password)
        .then(() => {
            loginError.textContent = "";
        })
        .catch(error => {
            loginError.textContent = error.message;
        });
}

// Load authors from Firestore
function loadAuthors() {
    db.collection('authors').get().then(snapshot => {
        authorsList = [];
        const tableBody = document.getElementById('authorsTable');
        tableBody.innerHTML = '';
        
        // Update authors dropdown in book form
        const authorSelect = document.getElementById('bookAuthor');
        authorSelect.innerHTML = '<option value="">Select author</option>';
        
        snapshot.forEach(doc => {
            const author = {id: doc.id, ...doc.data()};
            authorsList.push(author);
            
            // Add to table
            const row = `
                <tr>
                    <td>${author.id}</td>
                    <td>${author.name || 'No name'}</td>
                    <td>${author.email || 'No email'}</td>
                    <td>
                        <a href="${author.pfp}" target="_blank">View Image</a>
                    </td>
                    <td class="action-buttons">
                        <button class="btn btn-sm btn-info" onclick="viewAuthorDetails('${author.id}')">View</button>
                        <button class="btn btn-sm btn-primary" onclick="editAuthor('${author.id}')">Edit</button>
                        <button class="btn btn-sm btn-danger" onclick="deleteItem('${author.id}', 'author')">Delete</button>
                    </td>
                </tr>
            `;
            tableBody.innerHTML += row;
            
            // Add to dropdown
            const option = document.createElement('option');
            option.value = author.id;
            option.textContent = author.name;
            authorSelect.appendChild(option);
        });
    }).catch(error => {
        console.error("Error loading authors:", error);
        alert("Error loading authors: " + error.message);
    });
}

// Load books from Firestore
function loadBooks() {
    db.collection('books').get().then(snapshot => {
        const tableBody = document.getElementById('booksTable');
        tableBody.innerHTML = '';
        
        snapshot.forEach(doc => {
            const book = {id: doc.id, ...doc.data()};
            
            // Find author name
            let authorName = "Unknown";
            const author = authorsList.find(a => a.id === book.author);
            if (author) {
                authorName = author.name;
            }
            
            // Add to table
            const row = `
                <tr>
                    <td>${book.id}</td>
                    <td>${book.title}</td>
                    <td>${authorName}</td>
                    <td>${book.price}</td>
                    <td>${book.rating || '0.0'}</td>
                    <td class="action-buttons">
                        <button class="btn btn-sm btn-info" onclick="viewBookDetails('${book.id}')">View</button>
                        <button class="btn btn-sm btn-primary" onclick="editBook('${book.id}')">Edit</button>
                        <button class="btn btn-sm btn-danger" onclick="deleteItem('${book.id}', 'book')">Delete</button>
                    </td>
                </tr>
            `;
            tableBody.innerHTML += row;
        });
    }).catch(error => {
        console.error("Error loading books:", error);
        alert("Error loading books: " + error.message);
    });
}

// Load users from Firestore
function loadUsers() {
    db.collection('users').get().then(snapshot => {
        const tableBody = document.getElementById('usersTable');
        tableBody.innerHTML = '';
        
        snapshot.forEach(doc => {
            const user = {id: doc.id, ...doc.data()};
            
            // Add to table
            const row = `
                <tr>
                    <td>${user.id}</td>
                    <td>${user.name || 'No name'}</td>
                    <td>${user.email}</td>
                    <td>${user.role || 'user'}</td>
                    <td>${user.value || '0.00'}</td>
                    <td class="action-buttons">
                        <button class="btn btn-sm btn-info" onclick="viewUserDetails('${user.id}')">View</button>
                        <button class="btn btn-sm btn-primary" onclick="editUser('${user.id}')">Edit</button>
                        <button class="btn btn-sm btn-danger" onclick="deleteItem('${user.id}', 'user')">Delete</button>
                    </td>
                </tr>
            `;
            tableBody.innerHTML += row;
        });
    }).catch(error => {
        console.error("Error loading users:", error);
        alert("Error loading users: " + error.message);
    });
}

// View author details
function viewAuthorDetails(authorId) {
    db.collection('authors').doc(authorId).get().then(doc => {
        if (doc.exists) {
            const author = doc.data();
            
            // Display author details
            const detailsHTML = `
                <div class="row">
                    <div class="col-md-4">
                        <img src="${author.pfp}" alt="${author.name}" class="img-fluid mb-3">
                    </div>
                    <div class="col-md-8">
                        <h4>${author.name}</h4>
                        <p><strong>Email:</strong> ${author.email || 'Not provided'}</p>
                        <p><strong>Description:</strong></p>
                        <p>${author.description || 'No description available'}</p>
                    </div>
                </div>
            `;
            
            document.getElementById('authorDetailsBody').innerHTML = detailsHTML;
            viewAuthorModalBS.show();
        }
    }).catch(error => {
        console.error("Error getting author details:", error);
        alert("Error getting author details: " + error.message);
    });
}

// View book details
function viewBookDetails(bookId) {
    db.collection('books').doc(bookId).get().then(doc => {
        if (doc.exists) {
            const book = doc.data();
            
            // Find author name
            let authorName = "Unknown";
            const author = authorsList.find(a => a.id === book.author);
            if (author) {
                authorName = author.name;
            }
            
            // Display book details
            const detailsHTML = `
                <div class="row">
                    <div class="col-md-4">
                        <img src="${book.cover}" alt="${book.title}" class="img-fluid mb-3">
                        <p><strong>Price:</strong> ${book.price}</p>
                        <p><strong>Physical Price:</strong> ${book.pricePhysic || 'N/A'}</p>
                        <p><strong>Rating:</strong> ${book.rating || '0.0'} (${book.ratingCount || '0'} ratings)</p>
                        <p><strong>Trending:</strong> ${book.trending ? 'Yes' : 'No'}</p>
                    </div>
                    <div class="col-md-8">
                        <h4>${book.title}</h4>
                        <p><strong>Author:</strong> ${authorName}</p>
                        <p><strong>Description:</strong></p>
                        <p>${book.description}</p>
                        <p><strong>Content:</strong> <a href="${book.content}" target="_blank">View/Download</a></p>
                    </div>
                </div>
            `;
            
            document.getElementById('bookDetailsBody').innerHTML = detailsHTML;
            viewBookModalBS.show();
        }
    }).catch(error => {
        console.error("Error getting book details:", error);
        alert("Error getting book details: " + error.message);
    });
}

// View user details
function viewUserDetails(userId) {
    db.collection('users').doc(userId).get().then(doc => {
        if (doc.exists) {
            const user = doc.data();
            
            let favouriteBooks = '';
            if (user.favourite) {
                favouriteBooks = '<ul>';
                Object.values(user.favourite).forEach(bookId => {
                    favouriteBooks += `<li>${bookId}</li>`;
                });
                favouriteBooks += '</ul>';
            } else {
                favouriteBooks = '<p>No favourite books</p>';
            }
            
            let continueReading = '';
            if (user.continue) {
                continueReading = '<ul>';
                Object.entries(user.continue).forEach(([key, bookId]) => {
                    continueReading += `<li>${bookId}</li>`;
                });
                continueReading += '</ul>';
            } else {
                continueReading = '<p>No books in continue reading</p>';
            }
            
            let ratings = '';
            if (user.ratings) {
                ratings = '<ul>';
                Object.entries(user.ratings).forEach(([bookId, rating]) => {
                    ratings += `<li>Book ${bookId}: ${rating} stars</li>`;
                });
                ratings += '</ul>';
            } else {
                ratings = '<p>No ratings</p>';
            }
            
            // Display user details
            const detailsHTML = `
                <div class="row">
                    <div class="col-md-6">
                        <p><strong>Name:</strong> ${user.name || 'No name'}</p>
                        <p><strong>Email:</strong> ${user.email}</p>
                        <p><strong>Role:</strong> ${user.role || 'user'}</p>
                        <p><strong>Value:</strong> ${user.value || '0.00'}</p>
                    </div>
                    <div class="col-md-6">
                        <div class="mb-3">
                            <h5>Favourite Books</h5>
                            ${favouriteBooks}
                        </div>
                        <div class="mb-3">
                            <h5>Continue Reading</h5>
                            ${continueReading}
                        </div>
                        <div class="mb-3">
                            <h5>Ratings</h5>
                            ${ratings}
                        </div>
                    </div>
                </div>
            `;
            
            document.getElementById('userDetailsBody').innerHTML = detailsHTML;
            viewUserModalBS.show();
        }
    }).catch(error => {
        console.error("Error getting user details:", error);
        alert("Error getting user details: " + error.message);
    });
}

// Edit book
function editBook(bookId) {
    currentEditMode = true;
    
    db.collection('books').doc(bookId).get().then(doc => {
        if (doc.exists) {
            const book = doc.data();
            
            // Populate form
            document.getElementById('bookId').value = bookId;
            document.getElementById('bookTitle').value = book.title || '';
            document.getElementById('bookAuthor').value = book.author || '';
            document.getElementById('bookPrice').value = book.price || '';
            document.getElementById('bookPricePhysic').value = book.pricePhysic || '';
            document.getElementById('bookDescription').value = book.description || '';
            document.getElementById('bookCover').value = book.cover || '';
            document.getElementById('bookContent').value = book.content || '';
            document.getElementById('bookTrending').checked = book.trending || false;
            
            // Show cover preview
            previewImage('bookCover', 'coverPreview');
            
            // Change modal title
            document.getElementById('addBookModalLabel').textContent = 'Edit Book';
            
            // Show modal
            bookModalBS.show();
        }
    }).catch(error => {
        console.error("Error getting book:", error);
        alert("Error getting book: " + error.message);
    });
}

// Edit author
function editAuthor(authorId) {
    currentEditMode = true;
    
    db.collection('authors').doc(authorId).get().then(doc => {
        if (doc.exists) {
            const author = doc.data();
            
            // Populate form
            document.getElementById('authorId').value = authorId;
            document.getElementById('authorName').value = author.name || '';
            document.getElementById('authorEmail').value = author.email || '';
            document.getElementById('authorDescription').value = author.description || '';
            document.getElementById('authorPfp').value = author.pfp || '';
            
            // Show profile picture preview
            previewImage('authorPfp', 'profilePicPreview');
            
            // Change modal title
            document.getElementById('addAuthorModalLabel').textContent = 'Edit Author';
            
            // Show modal
            authorModalBS.show();
        }
    }).catch(error => {
        console.error("Error getting author:", error);
        alert("Error getting author: " + error.message);
    });
}

// Edit user
function editUser(userId) {
    currentEditMode = true;
    
    db.collection('users').doc(userId).get().then(doc => {
        if (doc.exists) {
            const user = doc.data();
            
            // Populate form
            document.getElementById('userId').value = userId;
            document.getElementById('userName').value = user.name || '';
            
            // Hide email field completely
            const emailField = document.getElementById('userEmail');
            const emailLabel = document.querySelector('label[for="userEmail"]');
            if (emailField) emailField.style.display = 'none';
            if (emailLabel) emailLabel.style.display = 'none';
            
            // Set email value in hidden field for validation
            if (emailField) emailField.value = user.email || '';
            
            // Show only role field
            document.getElementById('userRole').value = user.role || 'user';
            
            // Show only value field
            document.getElementById('userValue').value = user.value || '0.00';
            
            // Hide password field
            const passwordField = document.getElementById('userPassword');
            const passwordLabel = document.querySelector('label[for="userPassword"]');
            if (passwordField) passwordField.style.display = 'none';
            if (passwordLabel) passwordLabel.style.display = 'none';
            
            // Hide any other fields in the form
            const form = document.getElementById('addUserForm');
            for (let i = 0; i < form.elements.length; i++) {
                const element = form.elements[i];
                if (element.id !== 'userId' && 
                    element.id !== 'userName' && 
                    element.id !== 'userRole' && 
                    element.id !== 'userValue') {
                    
                    if (element.type !== 'hidden') {
                        const elementLabel = document.querySelector(`label[for="${element.id}"]`);
                        if (elementLabel) elementLabel.style.display = 'none';
                        element.style.display = 'none';
                    }
                }
            }
            
            // Change modal title
            document.getElementById('addUserModalLabel').textContent = 'Edit User';
            
            // Show modal
            userModalBS.show();
        }
    }).catch(error => {
        console.error("Error getting user:", error);
        alert("Error getting user: " + error.message);
    });
}

// Save book
function saveBook() {
    const bookId = document.getElementById('bookId').value;
    const bookTitle = document.getElementById('bookTitle').value.trim();
    const bookAuthor = document.getElementById('bookAuthor').value.trim();
    const bookPrice = document.getElementById('bookPrice').value.trim();
    const bookDescription = document.getElementById('bookDescription').value.trim();
    const bookCover = document.getElementById('bookCover').value.trim();
    const bookContent = document.getElementById('bookContent').value.trim();
    
    // Validation
    if (!bookTitle || !bookAuthor || !bookPrice || !bookDescription || !bookCover || !bookContent) {
        alert("Please fill in all required fields");
        return;
    }
    
    const bookData = {
        title: bookTitle,
        author: bookAuthor,
        price: bookPrice,
        pricePhysic: document.getElementById('bookPricePhysic').value.trim() || '',
        description: bookDescription,
        cover: bookCover,
        content: bookContent,
        trending: document.getElementById('bookTrending').checked
    };
    
    // Add default rating values for new books
    if (!currentEditMode) {
        bookData.rating = '0.0';
        bookData.ratingAverage = '0.0';
        bookData.ratingCount = '0';
    }
    
    let savePromise;
    if (bookId) {
        // Update existing book
        savePromise = db.collection('books').doc(bookId).update(bookData);
    } else {
        // Add new book
        savePromise = db.collection('books').add(bookData);
    }
    
    savePromise.then(() => {
        bookModalBS.hide();
        loadBooks();
    }).catch(error => {
        console.error("Error saving book:", error);
        alert("Error saving book: " + error.message);
    });
}

// Save author
function saveAuthor() {
    const authorId = document.getElementById('authorId').value;
    const authorName = document.getElementById('authorName').value.trim();
    const authorEmail = document.getElementById('authorEmail').value.trim();
    const authorPfp = document.getElementById('authorPfp').value.trim();
    const authorDescription = document.getElementById('authorDescription').value.trim();
    
    // Validation
    if (!authorName || !authorEmail || !authorPfp) {
        alert("Please fill in all required fields");
        return;
    }
    
    const authorData = {
        name: authorName,
        email: authorEmail,
        pfp: authorPfp,
        description: authorDescription
    };
    
    let savePromise;
    if (authorId) {
        // Update existing author
        savePromise = db.collection('authors').doc(authorId).update(authorData);
    } else {
        // Add new author
        savePromise = db.collection('authors').add(authorData);
    }
    
    savePromise.then(() => {
        authorModalBS.hide();
        loadAuthors();
    }).catch(error => {
        console.error("Error saving author:", error);
        alert("Error saving author: " + error.message);
    });
}

// Save user - modified to only allow editing name, role, and value
function saveUser() {
    // Get form field values with trimming to remove whitespace
    const userId = document.getElementById('userId').value.trim();
    const userName = document.getElementById('userName').value.trim();
    const userEmail = document.getElementById('userEmail').value.trim(); // Hidden but still needed for validation
    const userRole = document.getElementById('userRole').value.trim();
    const userValue = document.getElementById('userValue').value.trim() || '0.00';
    
    // This function now only supports editing existing users
    if (!userId) {
        alert("Adding new users has been disabled by administrator");
        return;
    }
    
    // Validation for required fields
    if (!userName || !userRole) {
        alert("Please fill in all required fields (Name and Role)");
        return;
    }
    
    // Get the current user data to compare
    db.collection('users').doc(userId).get()
        .then(doc => {
            if (!doc.exists) {
                throw new Error("User not found");
            }
            
            const currentUserData = doc.data();
            
            // Check if email is being changed
            if (userEmail !== currentUserData.email) {
                throw new Error("Changing email addresses is not allowed");
            }
            
            // Prepare update data - only name, role, and value
            const userData = {
                name: userName,
                role: userRole,
                value: userValue
            };
            
            // Update user
            return db.collection('users').doc(userId).update(userData);
        })
        .then(() => {
            userModalBS.hide();
            loadUsers();
            alert("User updated successfully");
        })
        .catch(error => {
            alert("Error: " + error.message);
        });
}

// Delete item setup
function deleteItem(id, type) {
    document.getElementById('deleteItemId').value = id;
    document.getElementById('deleteItemType').value = type;
    deleteModalBS.show();
}

// Confirm delete
function confirmDelete() {
    const id = document.getElementById('deleteItemId').value;
    const type = document.getElementById('deleteItemType').value;
    
    let deletePromise;
    switch (type) {
        case 'book':
            deletePromise = db.collection('books').doc(id).delete();
            break;
        case 'author':
            // Check if any books use this author
            db.collection('books').where('author', '==', id).get()
                .then(snapshot => {
                    if (!snapshot.empty) {
                        throw new Error(`Cannot delete author: ${snapshot.size} book(s) are using this author. Please reassign those books first.`);
                    }
                    return db.collection('authors').doc(id).delete();
                })
                .then(() => {
                    deleteModalBS.hide();
                    loadAuthors();
                })
                .catch(error => {
                    console.error("Error deleting author:", error);
                    alert(error.message);
                });
            return;
        case 'user':
            deletePromise = db.collection('users').doc(id).delete();
            break;
        default:
            alert("Invalid item type");
            deleteModalBS.hide();
            return;
    }
    
    deletePromise.then(() => {
        deleteModalBS.hide();
        
        // Reload appropriate data
        switch (type) {
            case 'book':
                loadBooks();
                break;
            case 'author':
                loadAuthors();
                loadBooks(); // Reload books as they might reference this author
                break;
            case 'user':
                loadUsers();
                break;
        }
    }).catch(error => {
        console.error(`Error deleting ${type}:`, error);
        alert(`Error deleting ${type}: ${error.message}`);
    });
}