# Agent Workspace Rules for Planora Project

When working on this workspace, please follow these guidelines carefully. They are essential for maintaining the application's Offline-First architecture, code quality, and preventing data leaks.

1. **Database Schema & Data Leak Prevention**: 
   - Whenever you add a new table to `PlanoraDatabase.sq`, you MUST create a delete query for it (e.g., `deleteAllMyTable: DELETE FROM myTableEntity;`) in the `.sq` file.
   - You MUST then add this delete query to the `PlanoraDatabaseExt.clearAll()` extension function located in `core/src/commonMain/kotlin/com/yusufteker/planora/core/database/PlanoraDatabaseExt.kt`.
   - Failing to do so will cause data leaks between accounts because the database won't clear correctly on logout.

2. **Ktor Token Management & Networking**:
   - Do NOT use Ktor's `Auth` plugin `loadTokens` cache for managing session persistence. 
   - Ensure the token is attached via the `requestPipeline.intercept` in `HttpClientProvider.kt` using `SessionPreferences`. This guarantees the most up-to-date token is used instantly upon account switching.

3. **ViewModels & Dependency Injection**:
   - Always ensure ViewModels are instantiated via `viewModel` or `factory` in Koin and not as singletons. Only Repositories and stateless UseCases should be `single`.

4. **Offline-First Architecture Requirements**:
   - Never update the UI directly from a network response.
   - All network responses MUST be saved to the local SQLDelight database first.
   - Flow/StateFlow should observe the local database as the Single Source of Truth.
   - When creating or updating entities locally before syncing, set their `isSynced` flag to `0L` and use a temporary local ID if creating (e.g., `local_uuid`). Only overwrite existing database entries from remote fetches if the local entry's `isSynced` is `1L` (already synced).

5. **Task Mapper Updates**:
   - Whenever you add a new field to `TaskEntity`, `TaskDto`, or `CreateTaskRequest`, you MUST update the mapping functions inside `feature-home/src/commonMain/kotlin/com/yusufteker/planora/feature/home/data/mapper/TaskMapper.kt`.
   - Failing to do so will result in data loss during local database saves or remote syncs.

6. **UI Localization (TR & EN support)**:
   - NEVER hardcode text strings directly in Jetpack Compose UI code.
   - All UI text, prompts, alerts, and button titles MUST be localized in both English and Turkish.
   - Default English strings MUST be added to `core/src/commonMain/composeResources/values/strings.xml`.
   - Turkish translation strings MUST be added to `core/src/commonMain/composeResources/values-tr/strings.xml`.
   - In UI screens, access these strings using `stringResource(Res.string.your_string_key)`.

7. **Code Documentation**:
   - All new classes, interfaces, repositories, use cases, and public functions MUST include KDoc/documentation.
   - Explain clearly what the class/function does, its parameters (inputs), return values (outputs), and any non-obvious business logic or architectural decisions.

8. **Further Reading**:
   - Please refer to `DEVELOPMENT_GUIDE.md` in the project root for comprehensive architectural details, request/response examples, offline sync mechanisms, and full service descriptions. Always consult it when adding major features.
   - 
9. # Design Rules

   ## General
   
   - Treat the application as a production-grade mobile product.
     - Do not create generic AI-generated UI.
     - Maintain one coherent visual language across the entire application.
     - Do not redesign individual screens independently.
     - Reuse existing design tokens and components whenever possible.
     - Do not introduce new colors, typography, spacing, radii, or components without a clear reason.
   
   ## Visual Direction
   
   The UI should feel:
   
   - Premium
     - Calm
     - Modern
     - Minimal
     - Trustworthy
     - Polished
   
   Prioritize:
   
   - Strong visual hierarchy
     - Excellent typography
     - Intentional whitespace
     - Clear primary actions
     - Consistent spacing
     - Restrained color palette
     - Subtle depth
     - Appropriate information density
     - Polished micro-interactions
   
   Avoid:
   
   - Generic gradients
     - Excessive rounded cards
     - Excessive glassmorphism
     - Random shadows
     - Too many colors
     - Giant headings
     - Excessive whitespace
     - Excessive use of cards
     - Generic "AI startup" aesthetics
     - Desktop/web-dashboard layouts on mobile
     - Inconsistent corner radii
     - Inconsistent spacing
   
   ## Mobile
   
   Design mobile-first for Android and iOS.
   
   Use:
   
   - Thumb-friendly interactions
     - Appropriate touch targets
     - Bottom sheets when appropriate
     - Native navigation patterns
     - Proper system insets
     - Keyboard-aware layouts
     - Gesture-friendly interactions
   
   Do not simply shrink a desktop UI into a mobile screen.
   
   ## Design System
   
   Before creating a new screen, check the existing design system.
   
   Reuse:
   
   - Colors
     - Typography
     - Spacing
     - Corner radii
     - Buttons
     - Inputs
     - Cards
     - Lists
     - Dialogs
     - Bottom sheets
     - Navigation components
     - Loading states
     - Empty states
     - Error states
     - Success states
   
   If a required component does not exist, design it consistently with the existing system.
   
   ## Screen Design
   
   Every screen must have:
   
   - Clear visual hierarchy
     - One obvious primary action
     - Appropriate information density
     - Loading state
     - Empty state where applicable
     - Error state where applicable
     - Success/confirmation state where applicable
   
   Do not add UI elements merely to make the screen look full.
   
   Every element must have a product reason.
   
   ## Before Implementation
   
   Before implementing a major new screen:
   
   1. Understand the user goal.
      2. Review the existing design system.
      3. Review existing screens for consistency.
      4. Define the information hierarchy.
      5. Consider loading, empty and error states.
      6. Then implement.
   
   ## Design Review
   
   After implementing a major UI:
   
   Act as an independent senior product designer and review the result.
   
   Look for:
   
   - Weak visual hierarchy
     - Inconsistent spacing
     - Poor typography
     - Unnecessary components
     - Excessive cards
     - Weak primary action
     - Confusing navigation
     - Accessibility issues
     - Generic AI-generated patterns
     - Inconsistent visual language
   
      Fix issues that materially improve the product.
   
      Do not change things merely for novelty.

10. **Premium & Free Plan Synchronization (Plan Comparison Matrix)**:
    - Whenever you add, update, or remove a Premium-exclusive feature, quota, retention period, or free-tier limitation in the application:
      1. You MUST update the comparison items in .
      2. You MUST update or add the corresponding localized strings in both  and .
      3. You MUST update the variable reference in .
    - The Plan Comparison screen () must ALWAYS accurately reflect the true production differences and limits between Free and Premium accounts.
