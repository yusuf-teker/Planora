import re

with open("server/src/main/kotlin/com/yusufteker/pulse/server/routes/TaskRoutes.kt", "r") as f:
    content = f.read()

imports = """import org.jetbrains.exposed.sql.update
import com.yusufteker.pulse.server.database.tables.UsersTable
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
"""
# find the first import and add our imports before it
first_import_idx = content.find("import ")
content = content[:first_import_idx] + imports + content[first_import_idx:]

with open("server/src/main/kotlin/com/yusufteker/pulse/server/routes/TaskRoutes.kt", "w") as f:
    f.write(content)
