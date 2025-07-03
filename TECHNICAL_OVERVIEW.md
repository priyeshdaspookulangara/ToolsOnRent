# ToolsOnRent - Technical Overview

This document provides a technical overview of the ToolsOnRent Android application, intended to help developers understand its structure, database, and core functionalities for future updates and feature additions.

## 1. Project Overview

*   **Project Name:** ToolsOnRent
*   **Purpose:** An Android application designed for managing the rental of tools. It helps track tools, customers, and rental transactions, facilitating efficient inventory and rental management.
*   **Key Technologies:**
    *   **Programming Language:** Kotlin
    *   **UI:** Android Jetpack Compose for building a modern, declarative user interface.
    *   **Database:** Room Persistence Library (SQLite object mapping) for robust local data storage.
    *   **Navigation:** AndroidX Navigation Component for handling in-app navigation between different screens.
    *   **Image Loading:** Glide for efficiently loading, caching, and displaying images (primarily for tool photos).
    *   **Calendar View:** Material Calendar View (`com.github.ProlificInteractive:material-calendarview`) for integrating calendar functionalities, likely for selecting rental dates or viewing schedules.
    *   **Build System:** Gradle with Kotlin DSL (`.gradle.kts`).

## 2. Database Schema

The application uses a local Room database. The schema consists of three main entities:

### 2.1. `Customer` Entity (`customers` table)
Stores information about customers.
*   `id` (Integer): Primary Key, Auto-generated.
*   `name` (String): Name of the customer. Not nullable.
*   `phoneNumber` (String): Phone number of the customer. Not nullable.
*   `email` (String): Email address of the customer. Nullable.
*   `address` (String): Physical address of the customer. Nullable.

### 2.2. `Tool` Entity (`tools` table)
Stores information about rentable tools.
*   `id` (Integer): Primary Key, Auto-generated.
*   `name` (String): Name of the tool. Not nullable.
*   `description` (String): Description of the tool. Nullable.
*   `rentalPrice` (Double): Rental price per day for the tool. Not nullable.
*   `totalQuantity` (Integer): The total number of units available for this tool. Not nullable.
*   `currentAvailableQuantity` (Integer): The number of units currently available for rent. Not nullable. (This value is expected to be updated upon new rentals and returns).
*   `imageUri` (String): URI string for the tool's image. Nullable.

### 2.3. `RentalTransaction` Entity (`rental_transactions` table)
Links tools and customers for each rental, acting as a historical record.
*   `id` (Integer): Primary Key, Auto-generated.
*   `toolId` (Integer): Foreign Key referencing `Tool.id`. Not nullable.
    *   `onDelete = ForeignKey.RESTRICT`: Prevents deleting a `Tool` if it has associated rental transactions.
    *   `onUpdate = ForeignKey.CASCADE`: If a `Tool.id` is updated, the change cascades to this column.
*   `customerId` (Integer): Foreign Key referencing `Customer.id`. Not nullable.
    *   `onDelete = ForeignKey.RESTRICT`: Prevents deleting a `Customer` if they have associated rental transactions.
    *   `onUpdate = ForeignKey.CASCADE`: If a `Customer.id` is updated, the change cascades to this column.
*   `rentalDate` (Date): The date when the rental started. Not nullable.
*   `dueDate` (Date): The date when the tool is due to be returned. Not nullable.
*   `returnDate` (Date): The actual date when the tool was returned. Nullable (null if not yet returned).
*   `rentalPricePerDay` (Double): The rental price per day for the tool *at the time of this specific transaction*. This ensures historical accuracy if tool prices change. Not nullable.
*   `notes` (String): Any additional notes related to this rental transaction. Nullable.

### 2.4. Relationships & Converters
*   **Type Converters:**
    *   `DateConverter`: Used by Room to convert `java.util.Date` objects (for `rentalDate`, `dueDate`, `returnDate`) into a format suitable for SQLite storage (likely a Long timestamp) and back.
*   **Indices:**
    *   The `rental_transactions` table has indices on `toolId`, `customerId`, `rentalDate`, and `dueDate` to optimize query performance.

## 3. Core Functionalities (Inferred)

Based on the codebase structure (primarily `ui` package):

*   **Tool Management:** Add, edit, view tools (including image, quantity management).
*   **Customer Management:** Add, edit, view customer details.
*   **Rental Management:**
    *   Initiate new rental transactions (selecting tool, customer, dates).
    *   View and manage active/current rentals.
    *   Process tool returns, updating availability and transaction status.
*   **Reporting System:**
    *   View all rental transactions.
    *   Generate reports on customer rental history.
    *   Generate reports on individual tool rental history.
    *   View inventory reports (tool quantities, possibly status).
    *   Identify and list overdue rentals.
    *   Generate profit/loss reports based on rental income.
*   **Dashboard:** A central screen providing an overview, possibly with calendar integration for visualizing rental schedules or due dates.
*   **Data Backup & Restore:** Functionality to backup the application's local database and restore from a backup.
*   **Application Settings:** A dedicated screen for user-configurable settings.

## 4. Code Structure

The project is organized by feature and layer:

*   **`com.example.toolsonrent` (Root):**
    *   `MainActivity.kt`: Single entry point Activity hosting Jetpack Navigation.
*   **`com.example.toolsonrent.database`:** Room database setup.
    *   `AppDatabase.kt`: Database class.
    *   `converter/DateConverter.kt`: Type converter for `Date`.
    *   `dao/`: Data Access Objects (`CustomerDao`, `ToolDao`, `RentalTransactionDao`) for database interactions.
    *   `entity/`: Kotlin data classes (`Customer`, `Tool`, `RentalTransaction`) defining the schema.
*   **`com.example.toolsonrent.ui`:** UI layer, using Jetpack Compose and MVVM. Sub-packages by feature (e.g., `addtool`, `customer`, `dashboard`, `reports`).
    *   Each feature package typically contains:
        *   `*Fragment.kt`: (If any legacy XML-based UI parts or for Navigation Component hosting) or Composable screen functions.
        *   `*ViewModel.kt`: Handles UI logic and state for the corresponding screen.
        *   `*Adapter.kt`: (For `RecyclerView` if used) or Composable list functions.
*   **`com.example.toolsonrent.utils`:** Utility classes.
    *   `CryptoUtil.kt`: Cryptographic operations (specific use needs review).
    *   `ImageFileUtil.kt`: Image file management utilities.

**Architectural Patterns:**
*   **MVVM (Model-View-ViewModel):** Predominant pattern for UI architecture.
*   **Single Activity Architecture:** `MainActivity` hosts various Fragments/Composable screens.
*   **Jetpack Compose:** Used for UI development.

## 5. Build Configuration and Dependencies

Managed in `app/build.gradle.kts`.

*   **Android Configuration:** `minSdk = 24`, `targetSdk = 34`, `compileSdk = 34`.
*   **Build Features:** `viewBinding = true` (may be for legacy parts or interop).
*   **Release Build:** `isMinifyEnabled = true`, `shrinkResources = true` (ProGuard enabled).
*   **Key Dependencies:**
    *   AndroidX (Core, Lifecycle, Activity, Compose libraries)
    *   Room (Runtime, Compiler via KSP, Kotlin extensions)
    *   Navigation Component (Fragment & UI KTX)
    *   Material Calendar View
    *   Glide (Core, KSP for processing)
    *   JUnit, AndroidX Test, Espresso (for testing)

## 6. Permissions

Declared in `AndroidManifest.xml`:

*   **`android.permission.CAMERA`**: To access the device camera, likely for capturing tool images.
    *   **Note:** Runtime permission handling for Android 6.0+ is required.
*   **`FileProvider`:** Configured for secure file sharing (e.g., sharing camera-captured images). Path details are in `res/xml/file_paths.xml`.

## 7. Areas for Future Development/Exploration

*   **Security Review:** Investigate `CryptoUtil.kt` usage and ensure robust data protection.
*   **Error Handling:** Implement comprehensive and user-friendly error handling and logging.
*   **Test Coverage:** Expand unit, integration, and UI tests.
*   **UI/UX Refinement:** Ensure consistency, usability, and accessibility. Review Compose best practices.
*   **Performance Optimization:** Profile and optimize background tasks, database queries, and list rendering.
*   **Dependency Updates:** Regularly check and update dependencies.
*   **Feature Enhancements:** Consider barcode scanning, notifications, cloud sync, advanced financial tracking.
*   **`FileProvider` Paths:** Review `res/xml/file_paths.xml` for appropriate scope.
*   **`DateConverter` Timezone Handling:** Clarify and ensure correct timezone management if necessary.
*   **Compose State Management:** Review and optimize state handling in Composable functions and ViewModels.

This document should serve as a good starting point for any new developer or agent working on the ToolsOnRent project.
