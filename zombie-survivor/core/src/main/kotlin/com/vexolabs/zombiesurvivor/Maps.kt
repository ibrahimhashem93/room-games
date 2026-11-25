package com.vexolabs.zombiesurvivor

// ---- playable maps: distinct background, wreck layout and monster mix ----
enum class MapDef(
    val label: String,
    val textureFile: String,
    val spitterBias: Int,   // widens the spitter roll window
    val runnerBias: Int,    // widens the runner roll window
    val wrecks: Array<Triple<Float, Float, Float>>  // x, y, rotation
) {
    STREET(
        "Old Street", "map.jpg", 0, 0,
        arrayOf(
            Triple(420f, 700f, 15f), Triple(980f, 620f, -8f), Triple(1220f, 980f, 22f),
            Triple(560f, 990f, -18f), Triple(1350f, 700f, 5f)
        )
    ),
    GRAVEYARD(
        "Foggy Graveyard", "map-graveyard.png", 12, 0,
        arrayOf(
            Triple(300f, 400f, 3f), Triple(700f, 1200f, -6f), Triple(1100f, 500f, 10f),
            Triple(1300f, 1100f, -14f), Triple(520f, 860f, 7f), Triple(900f, 900f, 0f)
        )
    ),
    HIGHWAY(
        "Desert Highway", "map-highway.png", 0, 15,
        arrayOf(
            Triple(380f, 300f, 90f), Triple(760f, 640f, 85f), Triple(1150f, 990f, 95f),
            Triple(1450f, 1300f, 88f), Triple(200f, 1000f, 92f)
        )
    )
}
