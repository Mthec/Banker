rootProject.name = "Banker"
include(":BMLBuilder", ":FaceSetter", ":PlaceNpc", ":WurmTestingHelper")
project(":BMLBuilder").projectDir = file("../BMLBuilder")
project(":PlaceNpc").projectDir = file("../PlaceNpc")
project(":FaceSetter").projectDir = file("../FaceSetter")
project(":WurmTestingHelper").projectDir = file("../WurmTestingHelper")