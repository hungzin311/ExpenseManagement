# Expense Management Application - Báo Cáo Kỹ Thuật

## 📋 Tổng Quan Project

Đây là ứng dụng quản lý chi tiêu cá nhân được phát triển bằng Android với Kotlin. Ứng dụng cho phép người dùng theo dõi thu nhập, chi tiêu, đặt mục tiêu tiết kiệm và xem thống kê tài chính theo ngày/tuần/tháng.

### Các Tính Năng Chính
- ✅ Đăng ký/Đăng nhập với Firebase Authentication
- ✅ Thêm/Sửa/Xóa giao dịch (Thu nhập & Chi tiêu)
- ✅ Xem danh sách giao dịch theo tháng
- ✅ Thống kê chi tiêu theo ngày/tuần/tháng
- ✅ Quản lý mục tiêu tiết kiệm (Savings Goals)
- ✅ Xem thống kê theo danh mục
- ✅ Quản lý hồ sơ người dùng

---

## 🏗️ Kiến Trúc và Kỹ Thuật

### 1. Kiến Trúc Ứng Dụng

Project sử dụng kiến trúc **Repository Pattern** kết hợp với **Activity-based Navigation**:

```
┌─────────────────────────────────────────┐
│         UI Layer (Activities)          │
│  - SignInActivity, HomeActivity, etc.  │
└──────────────┬──────────────────────────┘
               │
┌──────────────▼──────────────────────────┐
│      Repository Layer                   │
│  - FirebaseRepository                   │
│  - Xử lý CRUD operations               │
└──────────────┬──────────────────────────┘
               │
┌──────────────▼──────────────────────────┐
│      Data Layer                         │
│  - Firebase Realtime Database           │
│  - Firebase Authentication              │
│  - (Toàn bộ dữ liệu: users, transactions, categories, goals) │
└─────────────────────────────────────────┘
```

**Đặc điểm:**
- **Repository Pattern**: Tách biệt logic truy cập dữ liệu khỏi UI
- **Single Source of Truth**: Firebase Realtime Database làm nguồn dữ liệu chính
- **Separation of Concerns**: Mỗi layer có trách nhiệm riêng biệt

### 2. Các Kỹ Thuật Android Chính

#### 2.1. View Binding & Data Binding
```kotlin
// View Binding - Tự động generate class từ XML layout
private lateinit var binding: ActivityHomeBinding

override fun onCreate(savedInstanceState: Bundle?) {
    binding = ActivityHomeBinding.inflate(layoutInflater)
    setContentView(binding.root)
    // Truy cập views qua binding thay vì findViewById
    binding.totalExpense.text = "$1000"
}
```

**Lợi ích:**
- Type-safe: Compile-time checking
- Null-safe: Không lo NullPointerException
- Performance: Nhanh hơn findViewById

#### 2.2. RecyclerView với Adapter Pattern
```kotlin
class TransactionAdapter(private var transactions: List<Transaction>) 
    : RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder>() {
    
    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        val transaction = transactions[position]
        // Bind data to views
    }
}
```

**Kỹ thuật:**
- **ViewHolder Pattern**: Tái sử dụng views, tối ưu memory
- **DiffUtil**: Có thể sử dụng để update list hiệu quả
- **ItemTouchHelper**: Swipe to delete (đã setup trong SpendsFragment)

#### 2.3. Fragments
```kotlin
// Embed Fragment trong Activity
supportFragmentManager.beginTransaction()
    .replace(R.id.spendsFragmentContainer, SpendsFragment())
    .commit()
```

**Sử dụng:**
- `SpendsFragment`: Hiển thị danh sách giao dịch trong HomeActivity
- Tách biệt UI logic, dễ reuse

#### 2.4. Kotlin Coroutines cho Async Operations
```kotlin
@OptIn(DelicateCoroutinesApi::class)
private fun fetchAll() {
    GlobalScope.launch {
        // Background thread
        val transactions = firebaseRepository.getTransactionsByUserId(userId!!)
        
        runOnUiThread {
            // Main thread - update UI
            updateDashboard()
        }
    }
}
```

**Kỹ thuật:**
- **Suspend functions**: `await()` cho Firebase operations
- **runOnUiThread**: Chuyển về main thread để update UI
- **GlobalScope**: Sử dụng cho background tasks (có thể cải thiện bằng ViewModelScope)

#### 2.5. Firebase Integration

**Firebase Authentication:**
```kotlin
auth.signInWithEmailAndPassword(email, pass)
    .addOnCompleteListener { task ->
        if (task.isSuccessful) {
            // Navigate to HomeActivity
        }
    }
```

**Firebase Realtime Database:**
```kotlin
// Repository pattern với suspend functions
suspend fun getTransactionsByUserId(userId: String): List<Transaction> {
    val snapshot = transactionsRef.get().await()
    return snapshot.children
        .mapNotNull { it.getValue(Transaction::class.java) }
        .filter { it.userId == userId }
}
```

**Cấu trúc Database:**
```
Firebase Database
├── users/
│   └── {userId}/
│       ├── id
│       ├── username
│       └── email
├── transactions/
│   └── {transactionId}/
│       ├── id
│       ├── label
│       ├── amount
│       ├── userId
│       └── transactionDate
└── categories/
    └── {categoryId}/
        ├── id
        ├── name
        ├── type
        └── userId
```

#### 2.6. Navigation Pattern

**Activity-based Navigation:**
```kotlin
// Explicit Intent
val intent = Intent(this, HomeActivity::class.java)
startActivity(intent)

// Với data
intent.putExtra("transaction", transaction)
startActivityForResult(intent, REQUEST_CODE)
```

**Bottom Navigation:**
```kotlin
bottomNavigationView.setOnItemSelectedListener { menuItem ->
    when (menuItem.itemId) {
        R.id.item_home -> { /* Navigate */ }
        R.id.item_savings -> { /* Navigate */ }
    }
}
```

#### 2.7. Data Models (Entity Classes)
```kotlin
data class Transaction(
    val id: Int = -1,
    val label: String = "",
    val amount: Double = 0.0,
    val transactionDate: String = "",
    val userId: String = "",
    var code: String = ""
) : Serializable
```

**Đặc điểm:**
- **Data classes**: Immutable by default, dễ serialize
- **Serializable**: Có thể pass qua Intent
- **Default values**: Giảm boilerplate code

#### 2.8. Đồng bộ Savings Goals với Firebase
```kotlin
// Repository: ghi/xoá Goal trực tiếp trên Realtime Database
suspend fun insertGoal(goal: SavingsGoal): Int { ... }
suspend fun updateGoal(goal: SavingsGoal) { ... }
suspend fun deleteGoal(goalId: Int) { ... }

// HomeActivity: khi xoá transaction điều chỉnh Goal -> cập nhật lại currentAmount
if (isGoalAdjustmentTransaction(transaction)) {
    updateGoalCurrentAmountForTransaction(transaction, revertAdjustment = true)
}
```

**Sử dụng:**
- Goals được lưu trên Firebase, mọi thiết bị đều đồng bộ tức thời.
- Hỗ trợ **add / update / delete** mục tiêu và tự động cân bằng `currentAmount` khi thao tác xoá transaction điều chỉnh.

---

## 🔄 Flow của Ứng Dụng

### 1. Authentication Flow

```
┌─────────────────┐
│ SignInActivity  │ (Launcher Activity)
└────────┬────────┘
         │
         ├─[Sign In Success]─→ HomeActivity
         │
         ├─[Register Link]─→ SignUpActivity
         │                    └─[Sign Up Success]─→ HomeActivity
         │
         └─[Forgot Password]─→ Send Reset Email
```

**Chi tiết:**
1. User mở app → `SignInActivity` (Launcher Activity)
2. Nhập email/password → Firebase Authentication
3. Success → Navigate to `HomeActivity`
4. Nếu chưa có account → `SignUpActivity`
5. Forgot password → Gửi email reset qua Firebase

### 2. Main Application Flow

```
HomeActivity (Main Screen)
    │
    ├─[Add Button]─→ AddTransactionActivity
    │                   ├─[Add Income]─→ AddIncomeActivity
    │                   └─[Add Expense]─→ AddIncomeActivity (isExpense=true)
    │
    ├─[Total Expense Card]─→ TotalExpensesActivity
    │                          └─ Xem thống kê theo ngày/tuần/tháng
    │
    ├─[Savings Button]─→ SavingsActivity
    │                     ├─ Xem danh sách Goals
    │                     ├─[Add Goal]─→ AddGoalActivity
    │                     └─[Edit Goal]─→ Dialog để update
    │
    └─[Bottom Navigation]
        ├─ Home (current)
        ├─ Savings → SavingsActivity
        ├─ Notification (TODO)
        └─ Settings → ProfileActivity
            └─[Logout]─→ SignInActivity
```

### 3. Transaction Flow

```
AddTransactionActivity
    │
    ├─[Add Income Card]─→ AddIncomeActivity
    │                      ├─ Chọn Category (Salary/Rewards)
    │                      ├─ Nhập Title, Amount
    │                      ├─ Chọn Date (Calendar Grid)
    │                      └─[Save]─→ FirebaseRepository.insertTransaction()
    │                                  └─ Navigate back
    │
    └─[Add Expense Card]─→ AddIncomeActivity (isExpense=true)
                           ├─ Chọn Category (Health/Grocery)
                           ├─ Nhập Title, Amount (số âm)
                           ├─ Chọn Date
                           └─[Save]─→ FirebaseRepository.insertTransaction()
```

**Data Flow:**
```
User Input → AddIncomeActivity
    ↓
Create Transaction Object
    ↓
FirebaseRepository.insertTransaction()
    ↓
Firebase Realtime Database
    ↓
HomeActivity.onResume() → fetchAll()
    ↓
Update UI (Dashboard + RecyclerView)
```

### 4. Data Fetching Flow

```
Activity Lifecycle
    │
    ├─ onCreate() → Setup UI, Initialize
    │
    ├─ onResume() → fetchAll()
    │                 │
    │                 └─ GlobalScope.launch {
    │                      val transactions = 
    │                        firebaseRepository.getTransactionsByUserId(userId)
    │                      runOnUiThread {
    │                        updateDashboard()
    │                        transactionAdapter.setData(transactions)
    │                      }
    │                    }
    │
    └─ onDestroy() → Cleanup
```

**Đặc điểm:**
- Fetch data mỗi khi Activity resume (đảm bảo data mới nhất)
- Background thread cho network operations
- Main thread cho UI updates

### 5. Savings Goals Flow

```
SavingsActivity
    │
    ├─ onCreate() → fetchSavingsData()
    │                ├─ Tính currentSavings = income - expense (tháng hiện tại)
    │                ├─ Load Goals từ FirebaseRepository.getGoalsByUserId()
    │                └─ Update UI (Progress, Totals, RecyclerView)
    │
    ├─[Add Goal FAB]─→ AddGoalActivity
    │                   └─[Save]─→ FirebaseRepository.insertGoal()
    │                              └─ onActivityResult() → Refresh
    │
    └─[Click Goal]─→ Edit Dialog
                      └─[Update Amount]─→ recordGoalAdjustment()
                                          ├─ Tạo transaction mới (Goal Deposit/Withdrawal)
                                          └─ Đồng bộ currentAmount + log transaction vào Firebase
```

**Đặc điểm:**
- Goals được đồng bộ hoàn toàn trên Firebase Realtime Database.
- Xoá transaction điều chỉnh từ HomeActivity sẽ tự hoàn tác `currentAmount` của goal liên quan.
- Tính toán savings vẫn dựa trên transactions tháng hiện tại.

---

## 📦 Các Thành Phần Chính

### 1. Activities

| Activity | Chức Năng |
|----------|-----------|
| `SignInActivity` | Đăng nhập, quên mật khẩu |
| `SignUpActivity` | Đăng ký tài khoản mới |
| `HomeActivity` | Màn hình chính, dashboard |
| `AddTransactionActivity` | Chọn loại giao dịch (Income/Expense) |
| `AddIncomeActivity` | Form thêm thu nhập/chi tiêu |
| `TotalExpensesActivity` | Thống kê chi tiêu theo thời gian |
| `SavingsActivity` | Quản lý mục tiêu tiết kiệm |
| `AddGoalActivity` | Thêm mục tiêu tiết kiệm |
| `ProfileActivity` | Thông tin user, logout |

### 2. Fragments

| Fragment | Chức Năng |
|----------|-----------|
| `SpendsFragment` | Hiển thị danh sách giao dịch trong HomeActivity |
| `CategoriesFragment` | Thống kê theo danh mục (có thể có) |

### 3. Adapters

| Adapter | Chức Năng |
|---------|-----------|
| `TransactionAdapter` | Hiển thị danh sách giao dịch trong RecyclerView |
| `GoalAdapter` | Hiển thị danh sách mục tiêu tiết kiệm |

### 4. Data Models

| Model | Mô Tả |
|-------|-------|
| `Transaction` | Giao dịch (thu nhập/chi tiêu) |
| `User` | Thông tin người dùng |
| `Category` | Danh mục giao dịch |
| `SavingsGoal` | Mục tiêu tiết kiệm |

### 5. Repository

| Class | Chức Năng |
|-------|-----------|
| `FirebaseRepository` | CRUD operations với Firebase Realtime Database |

---

## 🛠️ Công Nghệ và Thư Viện

### Core Technologies
- **Kotlin**: Ngôn ngữ lập trình chính
- **Android SDK**: Min SDK 26, Target SDK 33
- **Gradle (Kotlin DSL)**: Build system

### Android Jetpack Libraries
- **ViewBinding & DataBinding**: Binding views
- **Lifecycle Components**: Quản lý lifecycle
- **Navigation Component**: Navigation giữa screens (đã setup nhưng chủ yếu dùng Intent)
- **Fragment KTX**: Fragment extensions

### Firebase Services
- **Firebase Authentication**: Email/Password authentication
- **Firebase Realtime Database**: NoSQL database realtime
- **Firebase Analytics**: Analytics (optional)

### UI Libraries
- **Material Design Components**: Material UI components
- **MPAndroidChart**: Charts cho thống kê (v3.1.0)
- **RecyclerView**: List rendering
- **GridLayout**: Calendar grid layout

### Networking & Data
- **Retrofit**: HTTP client (đã include nhưng có thể chưa dùng)
- **Moshi**: JSON parsing (đã include)
- **Kotlin Coroutines**: Async programming
- **kotlinx-coroutines-play-services**: Coroutines cho Firebase

### Other
- **WorkManager**: Background tasks (đã include)

---

## 📱 Các Kỹ Thuật UI/UX

### 1. Material Design
- Material Card Views
- Material Buttons
- Material Text Fields
- Material Dialogs

### 2. Responsive Layout
- ConstraintLayout cho flexible layouts
- GridLayout cho calendar
- RecyclerView với LinearLayoutManager

### 3. User Feedback
- Toast messages cho thông báo
- Snackbar với Undo action (xóa transaction)
- Error banners trong SignInActivity
- Progress bars cho Savings Goals

### 4. Date Selection
- Custom Calendar Grid trong AddIncomeActivity
- Date picker với visual calendar

---

## 🔐 Security & Best Practices

### 1. Authentication
- Firebase Authentication (secure, không lưu password)
- Session management tự động

### 2. Data Validation
- Input validation trong forms
- Error handling cho Firebase operations

### 3. Code Organization
- Package structure theo feature
- Separation of concerns
- Repository pattern

### 4. Memory Management
- ViewBinding lifecycle (null check trong Fragments)
- Proper cleanup trong onDestroy()

---

## 🚀 Cách Chạy Project

### Yêu cầu
- Android Studio (latest version)
- JDK 8+
- Android SDK 26+
- Firebase project setup

### Setup
1. Clone project
2. Mở trong Android Studio
3. Thêm `google-services.json` vào `app/` folder
4. Sync Gradle
5. Run trên emulator hoặc device

### Cấu trúc Project
```
app/src/main/java/com/ict/expensemanagement/
├── adapter/          # RecyclerView Adapters
├── auth/            # Authentication Activities
├── data/
│   ├── entity/      # Data Models
│   └── repository/  # FirebaseRepository
├── goal/            # Savings Goals
├── stats/           # Statistics Fragments
└── transaction/     # Transaction Activities
```

---

## 📊 Điểm Mạnh và Có Thể Cải Thiện

### ✅ Điểm Mạnh
1. **Kiến trúc rõ ràng**: Repository pattern, separation of concerns
2. **Firebase Integration**: Authentication + Realtime Database
3. **Modern Android**: ViewBinding, Coroutines, Material Design
4. **User Experience**: Bottom navigation, swipe gestures, undo actions
5. **Data Models**: Clean data classes với Serializable

### 🔄 Có Thể Cải Thiện
1. **Architecture**: Có thể thêm ViewModel (MVVM pattern)
2. **Coroutines**: Thay GlobalScope bằng ViewModelScope/CoroutineScope
3. **Error Handling**: Thêm try-catch và error states
4. **Testing**: Unit tests, UI tests
5. **Offline Support**: Cache data local
6. **Navigation**: Sử dụng Navigation Component thay vì Intent
7. **Dependency Injection**: Dagger Hilt hoặc Koin
8. **State Management**: LiveData/StateFlow cho reactive UI

---

## 📝 Kết Luận

Project này thể hiện việc áp dụng các kỹ thuật Android hiện đại:
- ✅ Kotlin với Coroutines
- ✅ Firebase Backend
- ✅ Material Design
- ✅ Repository Pattern
- ✅ ViewBinding
- ✅ RecyclerView & Adapters
- ✅ Fragments

Đây là một foundation tốt cho một ứng dụng quản lý chi tiêu, có thể mở rộng thêm nhiều tính năng như:
- Biểu đồ thống kê chi tiết
- Export báo cáo
- Nhắc nhở thanh toán
- Multi-currency support
- Cloud backup/sync

---

**Tác giả**: [Tên của bạn]  
**Ngày**: [Ngày hiện tại]  
**Môn học**: Mobile Programming
