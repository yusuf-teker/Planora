# Agent Workspace Rules for Pulse Project

When working on this workspace, please follow these guidelines carefully:

1. **Database Schema Changes**: 
   - Whenever you add a new table to `PulsyDatabase.sq`, you MUST create a delete query for it (e.g., `deleteAllMyTable: DELETE FROM myTableEntity;`) in the `.sq` file.
   - You MUST then add this delete query to the `PulsyDatabaseExt.clearAll()` extension function located in `core/src/commonMain/kotlin/com/yusufteker/pulse/core/database/PulsyDatabaseExt.kt`.
   - Failing to do so will cause data leaks between accounts because the database won't clear correctly on logout.

2. **Ktor Token Management**:
   - Do NOT use Ktor's `Auth` plugin `loadTokens` cache for managing session persistence. 
   - Ensure the token is attached via the `requestPipeline.intercept` in `HttpClientProvider.kt` using `SessionPreferences`.

3. **ViewModels**:
   - Always ensure ViewModels are instantiated via `viewModel` or `factory` in Koin and not as singletons. Only Repositories should be `single`.

4. **Further Reading**:
   - Please refer to `DEVELOPMENT_GUIDE.md` in the project root for more context and rules regarding empty states and navigations.
