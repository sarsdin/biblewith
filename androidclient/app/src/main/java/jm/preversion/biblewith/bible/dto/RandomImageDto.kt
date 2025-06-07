package jm.preversion.biblewith.bible.dto

data class RandomImageDto(
    var urls: Urls
)

data class Urls(
    var regular: String,
    var small: String
)
