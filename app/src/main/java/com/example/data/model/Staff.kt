package com.example.data.model

data class Staff(
    val name: String,
    val phone: String,
    val branch: String
) {
    val displayName: String
        get() = "$name ($branch) - $phone"
}

object StaffDirectory {
    val list = listOf(
        Staff("TK BISWAS", "8001080080", "ALL"),
        Staff("LAXMI", "9883605917", "KNE"),
        Staff("CHEMER", "8676002200", "KNE"),
        Staff("KISHAN-5", "6207841890", "KNE"),
        Staff("CRP", "9647840067", "JPE"),
        Staff("JALPAI-13", "8101397763", "JPE"),
        Staff("CHEMER", "8436002200", "JPE"),
        Staff("UTTAMA", "7679751521", "COB"),
        Staff("STAFF-2", "8617047420", "COB"),
        Staff("CHEMER", "8514002200", "COB"),
        Staff("FLK-1", "9883623823", "FLK"),
        Staff("CHEMER", "8514001100", "FLK"),
        Staff("BIR2", "7501275078", "BIR"),
        Staff("FALA15", "6294178845", "BIR")
    )
}
