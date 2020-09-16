package spotify.bot.filter.remapper;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.wrapper.spotify.enums.AlbumGroup;
import com.wrapper.spotify.model_objects.specification.TrackSimplified;

import spotify.bot.util.data.AlbumGroupExtended;

@Component
public class RemixRemapper implements Remapper {

	private final static Pattern REMIX_MATCHER = Pattern.compile("\\b(RMX|REMIX|REMIXES)\\b", Pattern.CASE_INSENSITIVE);
	private final static double REMIX_SONG_COUNT_PERCENTAGE_THRESHOLD = 0.65;
	private final static double REMIX_SONG_COUNT_PERCENTAGE_THRESHOLD_LESSER = 0.2;

	@Override
	public AlbumGroupExtended getAlbumGroup() {
		return AlbumGroupExtended.REMIX;
	}

	/**
	 * Any non-extended album group qualifies as relevant for Remix remapping
	 */
	@Override
	public boolean isAllowedAlbumGroup(AlbumGroupExtended albumGroupExtended) {
		if (albumGroupExtended.equals(AlbumGroupExtended.APPEARS_ON)) {
			try {
				AlbumGroup albumGroup = albumGroupExtended.asAlbumGroup();
				return albumGroup != null;
			} catch (IllegalArgumentException e) {
				return false;
			}
		}
		return false;
	}

	/**
	 * Returns true if the release OR at least half of the release's tracks have
	 * "REMIX" in their titles (one word, case insenstive)
	 */
	@Override
	public boolean qualifiesAsRemappable(String albumTitle, List<TrackSimplified> tracks) {
		boolean hasRemixInTitle = REMIX_MATCHER.matcher(albumTitle).find();
		List<String> trackIds = tracks.stream().map(TrackSimplified::getName).collect(Collectors.toList());
		double trackCountRemix = trackIds.stream().filter(t -> REMIX_MATCHER.matcher(t).find()).count();
		double trackCount = trackIds.size();
		double remixPercentage = trackCountRemix / trackCount;
		if (hasRemixInTitle) {
			return remixPercentage > REMIX_SONG_COUNT_PERCENTAGE_THRESHOLD_LESSER;
		}
		return remixPercentage > REMIX_SONG_COUNT_PERCENTAGE_THRESHOLD;
	}
}
