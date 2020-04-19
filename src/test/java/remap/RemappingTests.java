package remap;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import com.wrapper.spotify.SpotifyApi;
import com.wrapper.spotify.model_objects.specification.Album;
import com.wrapper.spotify.model_objects.specification.TrackSimplified;

import spotify.bot.api.BotException;
import spotify.bot.api.SpotifyApiAuthorization;
import spotify.bot.api.SpotifyApiWrapper;
import spotify.bot.api.SpotifyCall;
import spotify.bot.api.services.TrackService;
import spotify.bot.config.BotConfigFactory;
import spotify.bot.config.ConfigUpdate;
import spotify.bot.config.database.DatabaseService;
import spotify.bot.config.database.DiscoveryDatabase;
import spotify.bot.filter.remapper.EpRemapper;
import spotify.bot.filter.remapper.LiveRemapper;
import spotify.bot.filter.remapper.RemixRemapper;
import spotify.bot.util.BotLogger;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = { BotLogger.class, TrackService.class, DiscoveryDatabase.class, DatabaseService.class, BotConfigFactory.class, ConfigUpdate.class, SpotifyApiWrapper.class, SpotifyApiAuthorization.class })
@EnableConfigurationProperties
public class RemappingTests {

	@Autowired
	private SpotifyApi spotifyApi;

	@Autowired
	private SpotifyApiAuthorization spotifyApiAuthorization;

	@Autowired
	private TrackService trackService;

	private static EpRemapper epRemapper;
	private static LiveRemapper liveRemapper;
	private static RemixRemapper remixRemapper;

	@Before
	public void createRemappers() {
		epRemapper = new EpRemapper();
		liveRemapper = new LiveRemapper();
		remixRemapper = new RemixRemapper();

		login();
	}

	private void login() {
		try {
			spotifyApiAuthorization.login();
		} catch (BotException e) {
			e.printStackTrace();
			fail("Couldn't log in to Spotify Web API!");
		}
	}

	///////////////

	private Album getAlbum(String albumId) {
		try {
			return SpotifyCall.execute(spotifyApi.getAlbum(albumId));
		} catch (BotException e) {
			return null;
		}
	}

	private List<TrackSimplified> getTracksOfSingleAlbum(Album album) {
		try {
			return SpotifyCall.executePaging(spotifyApi
				.getAlbumsTracks(album.getId())
				.limit(50));
		} catch (BotException e) {
			return null;
		}
	}

	///////////////////////////////

	@Test
	public void epPositive() {
		Album album;
		boolean qualifiesAsRemappable;

		album = getAlbum("5wMGdTWNzO3qqztd2MyKrr"); // Crimson Shadows - The Resurrection
		qualifiesAsRemappable = epRemapper.qualifiesAsRemappable(album.getName(), getTracksOfSingleAlbum(album));
		assertTrue(qualifiesAsRemappable);

		album = getAlbum("0Przlkc8VDMp8SDhC24Nvs"); // Gary Washington - Black Carpet EP
		qualifiesAsRemappable = epRemapper.qualifiesAsRemappable(album.getName(), getTracksOfSingleAlbum(album));
		assertTrue(qualifiesAsRemappable);

		album = getAlbum("4J0hkWvySY1xfL9oHyF3ql"); // Swallow the Sun - Lumina Aurea
		qualifiesAsRemappable = epRemapper.qualifiesAsRemappable(album.getName(), getTracksOfSingleAlbum(album));
		assertTrue(qualifiesAsRemappable);
	}

	@Test
	public void epNegative() {
		Album album;
		boolean qualifiesAsRemappable;

		album = getAlbum("5NNahOPVAbt5gSCMmRuHTG"); // Nightwish - Harvest
		qualifiesAsRemappable = epRemapper.qualifiesAsRemappable(album.getName(), getTracksOfSingleAlbum(album));
		assertFalse(qualifiesAsRemappable);

		album = getAlbum("4YfXSuoJWZGcTNGAkFK8cO"); // Green Day - Oh Yeah!
		qualifiesAsRemappable = epRemapper.qualifiesAsRemappable(album.getName(), getTracksOfSingleAlbum(album));
		assertFalse(qualifiesAsRemappable);

		album = getAlbum("3BOQrewswG2ePGkShTx389"); // Die Aerzte - Drei Mann - Zwei Songs
		qualifiesAsRemappable = epRemapper.qualifiesAsRemappable(album.getName(), getTracksOfSingleAlbum(album));
		assertFalse(qualifiesAsRemappable);

		album = getAlbum("0UaxFieNv1ccs2GCECCUGy"); // Billy Talent - I Beg To Differ (This Will Get Better)
		qualifiesAsRemappable = epRemapper.qualifiesAsRemappable(album.getName(), getTracksOfSingleAlbum(album));
		assertFalse(qualifiesAsRemappable);
	}

	///////////////////////////////

	@Test
	public void livePositive() {
		Album album;
		boolean qualifiesAsRemappable;

		album = getAlbum("6U2FX33shPcoezU7oZS0eW"); // Helloween - United Alive in Madrid
		qualifiesAsRemappable = liveRemapper.qualifiesAsRemappable(album.getName(), getTracksOfSingleAlbum(album), trackService);
		assertTrue(qualifiesAsRemappable);

		album = getAlbum("5tCkJBpsqSgjk1ZHvHdC2I"); // Haken - L+1VE
		qualifiesAsRemappable = liveRemapper.qualifiesAsRemappable(album.getName(), getTracksOfSingleAlbum(album), trackService);
		assertTrue(qualifiesAsRemappable);

		album = getAlbum("6kJuATIGbPYxHmRoWCC5IB"); // Carpenter Brut - CARPENTERBRUTLIVE
		qualifiesAsRemappable = liveRemapper.qualifiesAsRemappable(album.getName(), getTracksOfSingleAlbum(album), trackService);
		assertTrue(qualifiesAsRemappable);

		album = getAlbum("1C0CHLxgm1yWcR2pCaj0q7"); // Disturbed - Hold on to Memories (Live)
		qualifiesAsRemappable = liveRemapper.qualifiesAsRemappable(album.getName(), getTracksOfSingleAlbum(album), trackService);
		assertTrue(qualifiesAsRemappable);
	}

	@Test
	public void liveNegative() {
		Album album;
		boolean qualifiesAsRemappable;

		album = getAlbum("3WJZV73n2hL1Hd4ldmalZR"); // Alestorm - Captain Morgan's Revenge (10th Anniversary Edition)
		qualifiesAsRemappable = liveRemapper.qualifiesAsRemappable(album.getName(), getTracksOfSingleAlbum(album), trackService);
		assertFalse(qualifiesAsRemappable);

		album = getAlbum("4WXCtg5Qs7McMVarDVzSxd"); // Saltatio Mortis - Brot und Spiele - Klassik & Krawall (Deluxe)
		qualifiesAsRemappable = liveRemapper.qualifiesAsRemappable(album.getName(), getTracksOfSingleAlbum(album), trackService);
		assertFalse(qualifiesAsRemappable);

		album = getAlbum("12cGa7OeAt3BiN8F8Ec1uJ"); // The Unguided - And the Battle Royale
		qualifiesAsRemappable = liveRemapper.qualifiesAsRemappable(album.getName(), getTracksOfSingleAlbum(album), trackService);
		assertFalse(qualifiesAsRemappable);

		album = getAlbum("7hRRdRCPsoWCF2gJr7yPZR"); // Enter Shikari - The King
		qualifiesAsRemappable = liveRemapper.qualifiesAsRemappable(album.getName(), getTracksOfSingleAlbum(album), trackService);
		assertFalse(qualifiesAsRemappable);

		album = getAlbum("6dksdceqBM8roInjffIaZw"); // Apocalyptica - Live or Die
		qualifiesAsRemappable = liveRemapper.qualifiesAsRemappable(album.getName(), getTracksOfSingleAlbum(album), trackService);
		assertFalse(qualifiesAsRemappable);

		album = getAlbum("7d3PsJu4ozQs75MTgWlBGC"); // Santiano - Wie Zuhause (MTV Unplugged)
		qualifiesAsRemappable = liveRemapper.qualifiesAsRemappable(album.getName(), getTracksOfSingleAlbum(album), trackService);
		assertFalse(qualifiesAsRemappable);
	}

	///////////////////////////////

	@Test
	public void remixPositive() {
		Album album;
		boolean qualifiesAsRemappable;

		album = getAlbum("5HuNp4GZrOJCw4qYYY5WNp"); // GReeeN - Remixes
		qualifiesAsRemappable = remixRemapper.qualifiesAsRemappable(album.getName(), getTracksOfSingleAlbum(album));
		assertTrue(qualifiesAsRemappable);

		album = getAlbum("3kNq2tf4Pl2vE6nlnNVAMH"); // Rammstein - Auslaender (Remixes)
		qualifiesAsRemappable = remixRemapper.qualifiesAsRemappable(album.getName(), getTracksOfSingleAlbum(album));
		assertTrue(qualifiesAsRemappable);

		album = getAlbum("7hyxShr7rqBsaCbIuUjssI"); // Savlonic - Black Plastic : Recycled
		qualifiesAsRemappable = remixRemapper.qualifiesAsRemappable(album.getName(), getTracksOfSingleAlbum(album));
		assertTrue(qualifiesAsRemappable);

		album = getAlbum("4rtd4mDEbypHitSuDFWvvc"); // Emigrate - War
		qualifiesAsRemappable = remixRemapper.qualifiesAsRemappable(album.getName(), getTracksOfSingleAlbum(album));
		assertTrue(qualifiesAsRemappable);

		album = getAlbum("3QZHRDBNh1GHDY6MCBULQp"); // Pendulum - The Reworks
		qualifiesAsRemappable = remixRemapper.qualifiesAsRemappable(album.getName(), getTracksOfSingleAlbum(album));
		assertTrue(qualifiesAsRemappable);
	}

	@Test
	public void remixNegative() {
		Album album;
		boolean qualifiesAsRemappable;

		album = getAlbum("29jsaLZpeO5jQLtOVUgwdV"); // Lindemann - Mathematik
		qualifiesAsRemappable = remixRemapper.qualifiesAsRemappable(album.getName(), getTracksOfSingleAlbum(album));
		assertFalse(qualifiesAsRemappable);

		album = getAlbum("44YMtn2oYAHVXQGFOKyXkJ"); // Binary Division - Midnight Crisis
		qualifiesAsRemappable = remixRemapper.qualifiesAsRemappable(album.getName(), getTracksOfSingleAlbum(album));
		assertFalse(qualifiesAsRemappable);

		album = getAlbum("231la1R3Z2UWS0rjRTIm9U"); // Lindemann - Steh auf
		qualifiesAsRemappable = remixRemapper.qualifiesAsRemappable(album.getName(), getTracksOfSingleAlbum(album));
		assertFalse(qualifiesAsRemappable);

		album = getAlbum("1b7Yy5kprvU3YiJmRdt4Bf"); // Cradle of Filth - Cruelty and the Beast
		qualifiesAsRemappable = remixRemapper.qualifiesAsRemappable(album.getName(), getTracksOfSingleAlbum(album));
		assertFalse(qualifiesAsRemappable);

		album = getAlbum("1L3K9GVu9coTIckoCpD6S9"); // Perturbator - B-Sides & Remixes 1
		qualifiesAsRemappable = remixRemapper.qualifiesAsRemappable(album.getName(), getTracksOfSingleAlbum(album));
		assertFalse(qualifiesAsRemappable);
	}
}
