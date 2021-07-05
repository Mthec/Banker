rootProject.name = "Banker"
include(":BMLBuilder", ":CreatureCustomiser", ":PlaceNpc", ":WurmTestingHelper")
project(":BMLBuilder").projectDir = file("../BMLBuilder")
project(":PlaceNpc").projectDir = file("../PlaceNpc")
project(":CreatureCustomiser").projectDir = file("../CreatureCustomiser")
project(":WurmTestingHelper").projectDir = file("../WurmTestingHelper")