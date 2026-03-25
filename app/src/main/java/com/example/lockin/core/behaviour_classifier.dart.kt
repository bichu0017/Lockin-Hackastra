internal class BehaviorClassifier {
    fun classify(score: Double): String {
        if (score >= 70) return "Productive"
        if (score >= 40) return "Risk Zone"
        return "Distracted"
    }
}