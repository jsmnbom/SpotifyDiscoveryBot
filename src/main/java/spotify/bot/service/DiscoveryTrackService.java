package spotify.bot.service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import org.springframework.stereotype.Component;

import se.michaelthelin.spotify.model_objects.specification.AlbumSimplified;
import spotify.api.SpotifyApiException;
import spotify.services.TrackService;
import spotify.util.SpotifyOptimizedExecutorService;
import spotify.util.data.AlbumTrackPair;

@Component
public class DiscoveryTrackService {
  private final TrackService trackService;
  private final SpotifyOptimizedExecutorService spotifyOptimizedExecutorService;

  DiscoveryTrackService(TrackService trackService, SpotifyOptimizedExecutorService spotifyOptimizedExecutorService) {
    this.trackService = trackService;
    this.spotifyOptimizedExecutorService = spotifyOptimizedExecutorService;
  }

  public List<AlbumTrackPair> getTracksOfAlbums(List<AlbumSimplified> albums) {
    List<Callable<List<AlbumTrackPair>>> callables = new ArrayList<>();
    for (AlbumSimplified album : albums) {
      callables.add(() -> {
        try {
          AlbumTrackPair tracksOfSingleAlbum = trackService.getTracksOfSingleAlbum(album);
          return List.of(tracksOfSingleAlbum);
        } catch (SpotifyApiException e) {
          return List.of();
        }
      });
    }
    return spotifyOptimizedExecutorService.executeAndWait(callables);
  }
}
