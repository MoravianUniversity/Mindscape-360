package vr.app.data

data class Video(
    val id: String,
    val title: String,
    val description: String,
    val duration: String,
    val imageResourceName: String,
    val category: String,
    val assetFileName: String
)

data class WelcomeContent(
    val title: String,
    val subtitle: String,
    val instructions: List<String>,
    val helpText: String,
    val buttonText: String
)

object AppContent {
    fun getWelcomeContent() = WelcomeContent(
        title = "Welcome to Mindscape 360",
        subtitle = "Your journey to calm begins here...",
        instructions = listOf(
            "Remove your phone case",
            "Adjust brightness and sound for comfort",
            "Choose a mindfulness session",
            "Place your phone in your VR headset viewer"
        ),
        helpText = "Need help? Watch the quick start video",
        buttonText = "Start Mindfulness Session"
    )
}

object VideoData {
    val videos = listOf(
        Video("1","Quiet Stream","Quiet stream with mindfulness","6 min 30 sec","quietstream","Mindfulness","QuietStream.mp4"),
        Video("2","Rocky Beach","Rocky beach with mindfulness","6 min 40 sec","rockybeach","Mindfulness","RockyBeach.mp4"),
        Video("3","Serene Beach","Serene beach with mindfulness","7 min 22 sec","serenebeach","Mindfulness","SereneBeach.mp4"),
        Video("4","Stream at Beach","Stream at beach with mindfulness","6 min 04 sec","streamatbeach","Mindfulness","StreamAtBeach.mp4"),
        Video("5","Sunrise Forest","Sunrise forest with mindfulness","6 min 34 sec","sunriseforest","Mindfulness","SunriseForest.mp4"),
        Video("6","Tree of Life","Tree of life with mindfulness","8 min 01 sec","treeoflife","Mindfulness","TreeOfLife.mp4")
    )

    fun getVideosByCategory(category: String) = videos.filter { it.category == category }
    fun getAllCategories() = videos.map { it.category }.distinct()
}