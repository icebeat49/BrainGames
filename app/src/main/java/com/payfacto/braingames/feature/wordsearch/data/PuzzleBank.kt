package com.payfacto.braingames.feature.wordsearch.data

import android.content.Context
import com.payfacto.braingames.feature.wordsearch.model.Puzzle

object PuzzleBank {

    fun getPuzzles(context: Context): List<Puzzle> {
        val lang = context.resources.configuration.locales[0].language
        return if (lang == "fr") frenchPuzzles else englishPuzzles
    }

    private val frenchPuzzles = listOf(
        Puzzle("Les animaux",  "POUSSIN",   "Bébé poule"),
        Puzzle("Les animaux",  "REQUIN",    "Prédateur des mers"),
        Puzzle("Les animaux",  "BALEINE",   "Plus grand mammifère marin"),
        Puzzle("Les fruits",   "ABRICOT",   "Fruit d'été à noyau orangé"),
        Puzzle("Les fruits",   "CERISE",    "Petit fruit rouge sur queue"),
        Puzzle("Les couleurs", "VIOLET",    "Mélange de rouge et de bleu"),
        Puzzle("Les couleurs", "INDIGO",    "Entre le bleu et le violet"),
        Puzzle("Les sports",   "NATATION",  "Sport aquatique par excellence"),
        Puzzle("Les sports",   "CYCLISME",  "Sport de vélo en compétition"),
        Puzzle("La nature",    "MONTAGNE",  "Plus haute que la colline"),
        Puzzle("La nature",    "PRINTEMPS", "Saison des fleurs"),
        Puzzle("La nature",    "ISLANDE",   "Île volcanique au nord de l'Europe"),
        Puzzle("Les arts",     "MUSIQUE",   "Art des sons et des mélodies"),
        Puzzle("La cuisine",   "CHOCOLAT",  "Fait à partir de cacao"),
        Puzzle("Les insectes", "PAPILLON",  "Insecte aux belles ailes colorées")
    )

    private val englishPuzzles = listOf(
        Puzzle("Animals",  "ROOSTER",    "Male chicken"),
        Puzzle("Animals",  "DOLPHIN",    "Intelligent sea mammal"),
        Puzzle("Animals",  "PENGUIN",    "Bird that cannot fly"),
        Puzzle("Fruits",   "APRICOT",    "Orange stone fruit"),
        Puzzle("Fruits",   "COCONUT",    "Tropical fruit with white flesh"),
        Puzzle("Colors",   "VIOLET",     "Red plus blue"),
        Puzzle("Colors",   "INDIGO",     "Between blue and violet"),
        Puzzle("Sports",   "SWIMMING",   "Aquatic sport"),
        Puzzle("Sports",   "CYCLING",    "Sport on two wheels"),
        Puzzle("Nature",   "MOUNTAIN",   "Taller than a hill"),
        Puzzle("Nature",   "SPRINGTIME", "Season of flowers"),
        Puzzle("Nature",   "ICELAND",    "Volcanic northern island"),
        Puzzle("Arts",     "MUSIC",      "Art of sound and melody"),
        Puzzle("Food",     "CHOCOLATE",  "Made from cacao beans"),
        Puzzle("Insects",  "BUTTERFLY",  "Beautiful winged insect")
    )
}
