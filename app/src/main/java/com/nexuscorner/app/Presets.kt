package com.nexuscorner.app

object Presets {
    // largura, altura, dpi
    val groups: List<Pair<String, List<Triple<Int, Int, Int>>>> = listOf(
        "Minhas resoluções" to listOf(Triple(3125, 1500, 450), Triple(5000, 2317, 450), Triple(3455, 2320, 410), Triple(4250, 1912, 580), Triple(4000, 1800, 520), Triple(3200, 1400, 450), Triple(3800, 1850, 470), Triple(4000, 1990, 550), Triple(3400, 1500, 450)),
        "Celular 20:9 · tela longa" to listOf(Triple(1600, 720, 280), Triple(1800, 810, 320), Triple(2000, 900, 350), Triple(2400, 1080, 420), Triple(2800, 1260, 480), Triple(3200, 1440, 490), Triple(3600, 1620, 510), Triple(4000, 1800, 530), Triple(4400, 1980, 550), Triple(4800, 2160, 570)),
        "Celular 19,5:9" to listOf(Triple(1560, 720, 280), Triple(1800, 830, 320), Triple(2100, 970, 380), Triple(2340, 1080, 420), Triple(2800, 1292, 480), Triple(3200, 1476, 500), Triple(3600, 1662, 520), Triple(4000, 1846, 530), Triple(4400, 2030, 550), Triple(4800, 2216, 570)),
        "Celular 19:9" to listOf(Triple(1520, 720, 280), Triple(1800, 852, 330), Triple(2100, 994, 390), Triple(2280, 1080, 420), Triple(2800, 1326, 480), Triple(3200, 1516, 500), Triple(3600, 1706, 520), Triple(4000, 1894, 540), Triple(4400, 2084, 560), Triple(4800, 2274, 580)),
        "Celular 16:9 · clássico" to listOf(Triple(1280, 720, 280), Triple(1600, 900, 350), Triple(1920, 1080, 420), Triple(2400, 1350, 480), Triple(2880, 1620, 510), Triple(3200, 1800, 530), Triple(3520, 1980, 550), Triple(3840, 2160, 570), Triple(4400, 2476, 600), Triple(4800, 2700, 620)),
        "Tablet 16:10" to listOf(Triple(1280, 800, 310), Triple(1600, 1000, 390), Triple(1920, 1200, 470), Triple(2200, 1376, 490), Triple(2560, 1600, 510), Triple(2800, 1750, 520), Triple(3200, 2000, 550), Triple(3600, 2250, 580), Triple(4000, 2500, 600), Triple(4800, 3000, 650)),
        "Tablet 4:3" to listOf(Triple(1024, 768, 300), Triple(1280, 960, 370), Triple(1600, 1200, 470), Triple(2048, 1536, 500), Triple(2400, 1800, 530), Triple(2800, 2100, 560), Triple(3200, 2400, 590), Triple(3600, 2700, 620), Triple(4000, 3000, 650), Triple(4800, 3600, 710))
    )
}
