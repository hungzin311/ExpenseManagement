# Expense Management Application - Báo Cáo Kỹ Thuật

Ứng dụng quản lý chi tiêu cá nhân được phát triển bằng Android + Kotlin. Ứng dụng cho phép người dùng quản lý thu/chi, mục tiêu tiết kiệm và xem danh sách giao dịch. Backend sử dụng **Firebase Authentication** và **Firebase Realtime Database**.

## 📋 Tổng quan project

### Các tính năng hiện có
- ✅ Đăng ký/Đăng nhập với Firebase Authentication (Email/Password)
- ✅ Thêm giao dịch Thu nhập & Chi tiêu
- ✅ Xem danh sách giao dịch theo user và cập nhật dashboard ở màn Home
- ✅ Xoá giao dịch bằng thao tác **swipe** (vuốt sang phải) trong danh sách ở Home + **Undo**
- ✅ Quản lý mục tiêu tiết kiệm (Savings Goals):
  - Thêm goal
  - Cập nhật `currentAmount`
  - Khi chỉnh `currentAmount`, app tự tạo transaction điều chỉnh “Goal Deposit/Withdrawal - <goal>”
  - Khi xoá transaction điều chỉnh goal từ Home, `currentAmount` của goal được hoàn tác tương ứng
- ✅ Quản lý hồ sơ người dùng (Profile)

---

## 🏗️ Kiến trúc & tổ chức mã nguồn

Project tổ chức theo hướng **Repository Pattern** và điều hướng chủ yếu bằng **Activity + Intent**:

```
┌─────────────────────────────────────────┐
│         UI Layer (Activities/Fragment)  │
│  - SignInActivity, HomeActivity, ...    │
│  - SpendsFragment (list transactions)   │
└──────────────┬──────────────────────────┘
               │
┌──────────────▼──────────────────────────┐
│      Repository Layer                   │
│  - FirebaseRepository                   │
│  - CRUD: users/transactions/categories/ │
│    goals                                │
└──────────────┬──────────────────────────┘
               │
┌──────────────▼──────────────────────────┐
│      Data Layer (Firebase)              │
│  - Firebase Authentication              │
│  - Firebase Realtime Database           │
└─────────────────────────────────────────┘
```

**Điểm chính:**
- **Repository Pattern**: UI không gọi Firebase trực tiếp, mọi thao tác dữ liệu đi qua `FirebaseRepository`.
- **Coroutines**: dùng `lifecycleScope` + `Dispatchers.IO` để gọi Firebase trong background, tránh block UI thread.
- **Single source of truth**: dữ liệu chính lưu trên Realtime Database.

---

## 🧩 Các kỹ thuật Android chính (đúng hiện trạng code)

### 1) ViewBinding & DataBinding
Project bật cả 2 trong `app/build.gradle.kts`. Thực tế code sử dụng **ViewBinding**:

```kotlin
private lateinit var binding: ActivityHomeBinding

override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    binding = ActivityHomeBinding.inflate(layoutInflater)
    setContentView(binding.root)
}
```

### 2) RecyclerView + Adapter Pattern
- `TransactionAdapter`: hiển thị danh sách transactions
- `GoalAdapter`: hiển thị danh sách goals

### 3) Fragment trong HomeActivity
`HomeActivity` nhúng `SpendsFragment` để hiển thị danh sách giao dịch.

### 4) Swipe-to-delete (ItemTouchHelper)
`SpendsFragment` gắn `ItemTouchHelper` để vuốt phải item và gọi callback xoá về `HomeActivity`.

### 5) Kotlin Coroutines + lifecycleScope
Các thao tác Firebase được gọi theo mẫu:

```kotlin
lifecycleScope.launch {
    val data = withContext(Dispatchers.IO) {
        firebaseRepository.getTransactionsByUserId(uid)
    }
    // update UI
}
```

### 6) Firebase Tasks + await()
`FirebaseRepository` dùng `.await()` để thao tác Realtime Database theo kiểu suspend.

### 7) Điều hướng bằng Intent + Serializable
`Transaction` implement `Serializable` để truyền qua `Intent` sang `DetailedActivity`.

---

## 🔥 Firebase Integration

### Firebase Authentication
- Đăng nhập/đăng ký bằng Email/Password.
- UID của Firebase Auth được dùng làm khoá user trong Realtime Database (`/users/{uid}`).

### Firebase Realtime Database
#### Endpoint đang dùng
Ứng dụng hiện trỏ tới Realtime DB:
- `https://expensemanagement-94f79-default-rtdb.asia-southeast1.firebasedatabase.app`

Endpoint được cấu hình qua `BuildConfig.FIREBASE_DB_URL` (khai báo trong `app/build.gradle.kts`) và được dùng trong `FirebaseRepository`.

#### Cấu trúc dữ liệu (tham khảo)
```
users/
  {uid}/
    id: string
    username: string
    passwordHash: string
    email: string
    code: string

transactions/
  {transactionId}/
    id: number
    label: string
    amount: number
    description: string
    transactionDate: string (yyyy-MM-dd)
    userId: string
    code: string
    linkedGoalId: number | null

categories/
  {categoryId}/
    id: number
    name: string
    type: string ("Income" | "Expense")
    userId: string

goals/
  {goalId}/
    id: number
    title: string
    targetAmount: number
    currentAmount: number
    iconResId: number
    userId: string
```

---

## 🔄 Flow của ứng dụng

### 1) Authentication Flow
```
SignInActivity (Launcher)
   ├─ Sign In success → HomeActivity
   └─ Register → SignUpActivity
             └─ Sign Up success → SignInActivity
```

**Ghi chú:** Khi SignUp thành công, app tạo user trong Auth rồi lưu thêm thông tin user vào `/users/{uid}`.

### 2) Main Application Flow
```
HomeActivity
  ├─ (+) Add → AddTransactionActivity
  │            ├─ Add Income → AddIncomeActivity
  │            └─ Add Expense → AddIncomeActivity (isExpense=true)
  ├─ Total Expense Card → TotalExpensesActivity
  ├─ Savings → SavingsActivity
  │            └─ Add Goal → AddGoalActivity
  └─ Settings/Profile → ProfileActivity
```

### 3) Transaction Flow (Income/Expense)
```
AddIncomeActivity
  ├─ nhập Title/Amount/Date + chọn Category
  └─ Save → FirebaseRepository.insertTransaction()
            └─ finish() → quay lại màn trước
```

**Refresh dữ liệu sau khi thêm:**
- `HomeActivity.onResume()` gọi `fetchAll()` để reload transactions và update dashboard/list.
- `AddTransactionActivity.onResume()` gọi `fetchLatestTransactions()` để refresh list "Latest Entries" (màn Add).

### 4) Delete Flow (Swipe) + Undo
```
SpendsFragment (RecyclerView)
  └─ swipe right item → HomeActivity.onDeleteTransaction()
        ├─ FirebaseRepository.deleteTransaction()
        ├─ nếu là goal-adjustment → hoàn tác goal.currentAmount
        └─ show Snackbar "Undo"
             └─ Undo → FirebaseRepository.insertTransaction(deletedTransaction)
                     └─ nếu goal-adjustment → apply lại goal.currentAmount
```

### 5) Savings Goals Flow
```
SavingsActivity
  ├─ fetchSavingsData():
  │    - load transactions tháng hiện tại → tính currentSavings
  │    - load goals → update tổng tiến độ
  ├─ Add goal → AddGoalActivity → insertGoal()
  └─ Edit goal currentAmount:
       - updateGoal(currentAmount)
       - recordGoalAdjustment(): tạo transaction "Goal Deposit/Withdrawal - <goal>"
         (gắn linkedGoalId để đồng bộ khi xoá)
```

---

## 📦 Các thành phần chính

### 1) Activities
| Activity | Chức năng |
|----------|-----------|
| `SignInActivity` | Đăng nhập |
| `SignUpActivity` | Đăng ký tài khoản |
| `HomeActivity` | Dashboard + danh sách giao dịch (SpendsFragment) + xoá/undo |
| `AddTransactionActivity` | Chọn luồng thêm Income/Expense + hiển thị Latest Entries |
| `AddIncomeActivity` | Form thêm thu/chi (dùng `isExpense=true` cho chi) |
| `DetailedActivity` | Xem chi tiết một transaction |
| `TotalExpensesActivity` | Thống kê tổng chi |
| `SavingsActivity` | Mục tiêu tiết kiệm + tổng tiến độ |
| `AddGoalActivity` | Thêm goal |
| `ProfileActivity` | Thông tin user |

### 2) Fragments
| Fragment | Chức năng |
|----------|-----------|
| `SpendsFragment` | RecyclerView danh sách transactions trong Home + swipe-to-delete |

### 3) Repository
| Class | Chức năng |
|-------|-----------|
| `FirebaseRepository` | CRUD cho users/transactions/categories/goals trên Realtime Database |

### 4) Data models
| Model | Mô tả |
|-------|------|
| `User` | Thông tin user (lưu dưới `/users/{uid}`) |
| `Transaction` | Giao dịch; có `linkedGoalId` để map về goal khi hoàn tác |
| `Category` | Danh mục Income/Expense |
| `SavingsGoal` | Goal tiết kiệm (`targetAmount`, `currentAmount`) |

---

## 🛠️ Công nghệ & thư viện

### Core
- Kotlin
- Android SDK (minSdk 26)
- Gradle Kotlin DSL

### Android Jetpack
- ViewBinding + DataBinding
- Lifecycle (lifecycleScope)
- Fragment KTX
- RecyclerView

### Firebase
- Firebase Authentication
- Firebase Realtime Database
- Firebase Analytics

### Khác
- Kotlin Coroutines
- `kotlinx-coroutines-play-services` (await cho Firebase)
- MPAndroidChart (phục vụ thống kê)
- Retrofit + Moshi (đã khai báo dependency)
- WorkManager (đã khai báo dependency)

---

## 🚀 Cách chạy project

### Yêu cầu
- Android Studio
- JDK 8+
- Android SDK phù hợp (minSdk 26)

### Thiết lập Firebase
1. Trong Firebase Console, tạo/đăng ký Android app với package: `com.ict.expensemanagement`.
2. Tải `google-services.json` và đặt vào thư mục `app/`.
3. Sync Gradle.

> Khi đổi sang Firebase project khác (hoặc DB host khác), cần cập nhật `google-services.json` đúng project để Auth/DB hoạt động nhất quán.

### Build & Run
- Android Studio: Run trên emulator/device
- CLI (Windows PowerShell):
  - `./gradlew assembleDebug`

### Cấu trúc project (thư mục chính)
```
app/src/main/java/com/ict/expensemanagement/
├── adapter/
├── auth/
├── data/
│   ├── entity/
│   └── repository/
├── goal/
├── stats/
└── transaction/
```

---

## 📝 Kết luận
Project triển khai mô hình quản lý thu/chi với **Firebase Auth + Realtime Database**, tổ chức theo **Repository Pattern** và dùng **Coroutines** để xử lý bất đồng bộ. Ứng dụng đã có luồng quản lý giao dịch, xoá/undo, và đồng bộ mục tiêu tiết kiệm (goal) thông qua transaction điều chỉnh.
