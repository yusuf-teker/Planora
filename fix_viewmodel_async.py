import re

with open("feature-home/src/commonMain/kotlin/com/yusufteker/pulse/feature/home/presentation/task_editor/TaskEditorViewModel.kt", "r") as f:
    content = f.read()

# Make sure kotlinx.coroutines.async and awaitAll are imported
if "import kotlinx.coroutines.async" not in content:
    content = content.replace("import kotlinx.coroutines.launch", "import kotlinx.coroutines.launch\nimport kotlinx.coroutines.async\nimport kotlinx.coroutines.awaitAll")

old_code = """                        val profiles = room.members.mapNotNull { member ->
                            profileRepository.getProfile(member.userId.toString()).getOrNull()
                        }
                        _state.update { it.copy(roomMembers = profiles) }"""

new_code = """                        val deferredProfiles = room.members.map { member ->
                            kotlinx.coroutines.async {
                                profileRepository.getProfile(member.userId.toString()).getOrNull()
                            }
                        }
                        val profiles = deferredProfiles.awaitAll().filterNotNull()
                        _state.update { it.copy(roomMembers = profiles) }"""

content = content.replace(old_code, new_code)

with open("feature-home/src/commonMain/kotlin/com/yusufteker/pulse/feature/home/presentation/task_editor/TaskEditorViewModel.kt", "w") as f:
    f.write(content)
