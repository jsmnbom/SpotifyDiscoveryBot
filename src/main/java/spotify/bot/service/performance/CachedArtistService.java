package spotify.bot.service.performance;

import java.sql.SQLException;
import java.time.Instant;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.google.common.collect.ImmutableList;

import se.michaelthelin.spotify.model_objects.specification.AlbumSimplified;
import se.michaelthelin.spotify.model_objects.specification.Artist;
import spotify.api.BotException;
import spotify.bot.config.database.DatabaseService;
import spotify.bot.filter.FilterService;
import spotify.bot.service.DiscoveryAlbumService;
import spotify.bot.util.DiscoveryBotLogger;
import spotify.bot.util.data.CachedArtistsContainer;
import spotify.services.ArtistService;
import spotify.util.BotUtils;

/**
 * Performance service to cache the user's followed artists and only update them once every 24 hours.
 * This is because it's very unlikely that a user follows an artist and then the artist immediately
 * releases new material (i.e. in that 24-hour timeframe after the follow).
 */
@Service
public class CachedArtistService {
  private final ArtistService artistService;
  private final DatabaseService databaseService;
  private final DiscoveryAlbumService discoveryAlbumService;
  private final FilterService filterService;
  private final DiscoveryBotLogger log;

  private final static int ARTIST_CACHE_EXPIRATION_DAYS = 1;

  private Date artistCacheLastUpdated;

  CachedArtistService(ArtistService artistService, DatabaseService databaseService, FilterService filterService, DiscoveryAlbumService discoveryAlbumService, DiscoveryBotLogger discoveryBotLogger) {
    this.artistService = artistService;
    this.databaseService = databaseService;
    this.filterService = filterService;
    this.discoveryAlbumService = discoveryAlbumService;
    this.log = discoveryBotLogger;
    this.artistCacheLastUpdated = Date.from(Instant.ofEpochSecond(0));
  }

  /**
   * Get all the user's followed artists
   */
  public CachedArtistsContainer getFollowedArtistsIds() throws SQLException, BotException {
    List<String> cachedArtists = getCachedArtistIds();
    if (isArtistCacheExpired(cachedArtists)) {
      List<String> followedArtistIds = getRealArtistIds();
      if (followedArtistIds.isEmpty()) {
        throw new BotException(new IllegalArgumentException("No followed artists found!"));
      }
      filterService.cacheArtistIds(followedArtistIds, false);
      this.artistCacheLastUpdated = new Date();
      return repackageIntoContainer(followedArtistIds, cachedArtists);
    } else {
      return new CachedArtistsContainer(cachedArtists, ImmutableList.of());
    }
  }

  /**
   * Wrap everything into a container to determine which artists were newly added
   * (to initialize the album cache for them in a later step)
   */
  private CachedArtistsContainer repackageIntoContainer(List<String> followedArtist, List<String> oldCachedArtists) {
    Set<String> addedArtists = new HashSet<>(followedArtist);
    oldCachedArtists.forEach(addedArtists::remove); // apparently faster than removeAll()
    return new CachedArtistsContainer(followedArtist, addedArtists);
  }

  /**
   * Get the real artist IDs directly from the Spotify API
   */
  private List<String> getRealArtistIds() throws BotException {
    List<Artist> followedArtists = artistService.getFollowedArtists();
    List<String> followedArtistIds = followedArtists.stream()
        .map(Artist::getId)
        .collect(Collectors.toList());
    BotUtils.removeNullStrings(followedArtistIds);
    return followedArtistIds;
  }

  /**
   * Get the list of cached artists from the DB
   */
  private List<String> getCachedArtistIds() throws SQLException {
    List<String> cachedArtists = databaseService.getArtistCache();
    BotUtils.removeNullStrings(cachedArtists);
    return cachedArtists;
  }

  private boolean isArtistCacheExpired(List<String> cachedArtists) {
    if (cachedArtists == null || cachedArtists.isEmpty()) {
      return !BotUtils.isWithinTimeoutWindow(artistCacheLastUpdated, ARTIST_CACHE_EXPIRATION_DAYS);
    }
    return false;
  }

  /////////////

  public void initializeAlbumCacheForNewArtists(CachedArtistsContainer cachedArtistsContainer) throws SQLException {
    List<String> newArtists = cachedArtistsContainer.getNewArtists();
    if (!newArtists.isEmpty()) {
      log.info("Initializing album cache for " + newArtists.size() + " newly followed artist[s]:");
      log.info(artistService.getArtists(newArtists).stream()
          .map(Artist::getName)
          .sorted()
          .collect(Collectors.joining(", ")));
      List<AlbumSimplified> allAlbumsOfNewFollowees = discoveryAlbumService.getAllAlbumsOfArtists(newArtists);
      List<AlbumSimplified> albumsToInitialize = filterService.getNonCachedAlbums(allAlbumsOfNewFollowees);
      filterService.cacheAlbumIds(albumsToInitialize, false);
      filterService.cacheAlbumNames(albumsToInitialize, false);
    }
  }
}