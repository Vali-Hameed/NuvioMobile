package com.nuvio.app.features.home

import com.nuvio.app.features.addons.ManagedAddon
import com.nuvio.app.features.catalog.CatalogTarget
import com.nuvio.app.features.details.MetaExternalRating
import com.nuvio.app.features.watchprogress.ContinueWatchingItem
import com.nuvio.app.features.watchprogress.ContinueWatchingPreferencesRepository
import com.nuvio.app.features.watchprogress.ContinueWatchingPreferencesUiState
import com.nuvio.app.features.watchprogress.ContinueWatchingPriority
import com.nuvio.app.features.watchprogress.isSeriesContinueWatchingItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

data class MetaPreview(
    val id: String,
    val type: String,
    val name: String,
    val poster: String? = null,
    val banner: String? = null,
    val logo: String? = null,
    val posterShape: PosterShape = PosterShape.Poster,
    val description: String? = null,
    val releaseInfo: String? = null,
    val rawReleaseDate: String? = null,
    val popularity: Double? = null,
    val voteCount: Int? = null,
    val imdbRating: String? = null,
    val genres: List<String> = emptyList(),
    val rawPosterUrl: String? = null,
    val landscapePoster: String? = null,
    val rawLandscapePosterUrl: String? = null,
    val runtime: String? = null,
    val status: String? = null,
    val ageRating: String? = null,
    val language: String? = null,
    val externalRatings: List<MetaExternalRating> = emptyList(),
)

fun MetaPreview.stableKey(): String = "$type:$id"

enum class PosterShape {
    Poster,
    Square,
    Landscape,
}

data class HomeCatalogSection(
    val key: String,
    val title: String,
    val subtitle: String,
    val addonName: String,
    val target: CatalogTarget,
    val items: List<MetaPreview>,
    val availableItemCount: Int = items.size,
    val hasMore: Boolean = false,
)

fun HomeCatalogSection.canOpenCatalog(previewLimit: Int): Boolean =
    availableItemCount > previewLimit || hasMore

data class HomeUiState(
    val isLoading: Boolean = false,
    val heroItems: List<MetaPreview> = emptyList(),
    val sections: List<HomeCatalogSection> = emptyList(),
    val errorMessage: String? = null,
)

enum class ContinueWatchingMediaCategory {
    SERIES,
    MOVIES,
}

data class ContinueWatchingRail(
    val category: ContinueWatchingMediaCategory,
    val items: List<ContinueWatchingItem>,
)

sealed interface ContinueWatchingUiState {
    data class Combined(
        val items: List<ContinueWatchingItem>,
    ) : ContinueWatchingUiState

    data class Separate(
        val priority: ContinueWatchingPriority,
        val seriesItems: List<ContinueWatchingItem>,
        val movieItems: List<ContinueWatchingItem>,
    ) : ContinueWatchingUiState {
        val orderedRails: List<ContinueWatchingRail>
            get() = when (priority) {
                ContinueWatchingPriority.SERIES_FIRST -> listOf(
                    ContinueWatchingRail(ContinueWatchingMediaCategory.SERIES, seriesItems),
                    ContinueWatchingRail(ContinueWatchingMediaCategory.MOVIES, movieItems),
                )
                ContinueWatchingPriority.MOVIES_FIRST -> listOf(
                    ContinueWatchingRail(ContinueWatchingMediaCategory.MOVIES, movieItems),
                    ContinueWatchingRail(ContinueWatchingMediaCategory.SERIES, seriesItems),
                )
            }.filter { it.items.isNotEmpty() }
    }
}

internal fun List<ContinueWatchingItem>.filterContinueWatchingByMediaType(
    showSeries: Boolean = true,
    showMovies: Boolean = true,
): List<ContinueWatchingItem> {
    if (showSeries && showMovies) return this
    if (!showSeries && !showMovies) return emptyList()
    return filter { item ->
        if (item.isSeriesContinueWatchingItem()) showSeries else showMovies
    }
}

internal fun buildContinueWatchingUiState(
    items: List<ContinueWatchingItem>,
    isContinueWatchingSplit: Boolean,
    continueWatchingPriority: ContinueWatchingPriority = ContinueWatchingPriority.SERIES_FIRST,
    showSeries: Boolean = true,
    showMovies: Boolean = true,
): ContinueWatchingUiState {
    val filteredItems = items.filterContinueWatchingByMediaType(
        showSeries = showSeries,
        showMovies = showMovies,
    )
    if (!isContinueWatchingSplit) {
        return ContinueWatchingUiState.Combined(items = filteredItems)
    }
    val (seriesItems, movieItems) = filteredItems.partition(ContinueWatchingItem::isSeriesContinueWatchingItem)
    return ContinueWatchingUiState.Separate(
        priority = continueWatchingPriority,
        seriesItems = seriesItems,
        movieItems = movieItems,
    )
}

internal fun observeContinueWatchingUiState(
    watchHistoryFlow: Flow<List<ContinueWatchingItem>>,
    preferencesFlow: Flow<ContinueWatchingPreferencesUiState> = ContinueWatchingPreferencesRepository.uiState,
): Flow<ContinueWatchingUiState> = combine(
    watchHistoryFlow,
    preferencesFlow,
) { items, preferences ->
    buildContinueWatchingUiState(
        items = items,
        isContinueWatchingSplit = preferences.isContinueWatchingSplit,
        continueWatchingPriority = preferences.continueWatchingPriority,
        showSeries = preferences.showSeries,
        showMovies = preferences.showMovies,
    )
}

internal fun shouldShowInitialHomeLoading(
    hasRenderableHomeRows: Boolean,
    addonManifestsLoading: Boolean,
    homeCatalogLoading: Boolean,
): Boolean = !hasRenderableHomeRows && (addonManifestsLoading || homeCatalogLoading)

internal fun shouldShowHomeHeroSlot(
    heroEnabled: Boolean,
    hasHeroItems: Boolean,
    isResolvingHeroSources: Boolean,
    hasRenderableHomeRows: Boolean,
): Boolean = heroEnabled && (hasHeroItems || isResolvingHeroSources || hasRenderableHomeRows)

internal data class CatalogRequest(
    val addon: ManagedAddon,
    val catalogId: String,
    val catalogName: String,
    val type: String,
    val supportsPagination: Boolean,
)
