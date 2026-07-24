import re

with open("feature-home/src/commonMain/kotlin/com/yusufteker/planora/feature/home/presentation/task_editor/TaskEditorViewModel.kt", "r") as f:
    content = f.read()

old_load_1 = """                        val membersList = mutableListOf<com.yusufteker.planora.shared.api.UserProfileResponse>()
                        room.members.forEach { member ->
                            profileRepository.getProfile(member.userId.toString()).onSuccess { profile ->
                                membersList.add(profile)
                            }
                        }
                        _state.update { it.copy(roomMembers = membersList) }"""

new_load_1 = """                        val profiles = room.members.mapNotNull { member ->
                            profileRepository.getProfile(member.userId.toString()).getOrNull()
                        }
                        _state.update { it.copy(roomMembers = profiles) }"""

content = content.replace(old_load_1, new_load_1)

with open("feature-home/src/commonMain/kotlin/com/yusufteker/planora/feature/home/presentation/task_editor/TaskEditorViewModel.kt", "w") as f:
    f.write(content)
