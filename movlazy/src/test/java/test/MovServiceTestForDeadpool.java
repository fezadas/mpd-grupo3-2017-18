/*
 * Copyright (c) 2017, Miguel Gamboa
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package test;

import com.google.common.util.concurrent.RateLimiter;
import movlazy.MovService;
import movlazy.MovWebApi;
import movlazy.dto.SearchItemDto;
import movlazy.model.CastItem;
import movlazy.model.SearchItem;
import org.junit.jupiter.api.Test;
import util.HttpRequest;
import util.IRequest;
import util.Queries;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static util.Queries.filter;
import static util.Queries.skip;

public class MovServiceTestForDeadpool {

    @Test
    public void testSearchMovieInSinglePage() {
        MovService movapi = new MovService(new MovWebApi(new HttpRequest()));
        Iterable<SearchItem> movs = movapi.search("Deadpool");
        SearchItem m = movs.iterator().next();
        assertEquals("Deadpool", m.getTitle());
        assertEquals(7, Queries.count(movs));// number of returned movies
    }

    @Test
    public void testSearchMovieManyPages() {
        int[] count = {0};
        IRequest req = new HttpRequest()
                // .compose(System.out::println)
                .compose(__ -> count[0]++);

        MovService movapi = new MovService(new MovWebApi(req));
        Iterable<SearchItem> movs = movapi.search("games");
        assertEquals(0, count[0]);
        SearchItem candleshoe =
                filter(
                        m -> m.getTitle().equals("Deadly Games"),
                        movs)
                        .iterator()
                        .next();
        assertEquals(3, count[0]); // Found on 2nd page
        assertEquals(291, Queries.count(movs));// Number of returned movies
        assertEquals(19, count[0]); //TODO: perceber os 4 requests more to consume all pages. 15 pages
    }

    @Test
    public void testMovieDbApiGetActor() {
        int[] count = {0};
        IRequest req = new HttpRequest()
                // .compose(System.out::println)
                .compose(__ -> count[0]++);

        MovWebApi movWebApi = new MovWebApi(req);
        SearchItemDto[] actorMovs = movWebApi.getPersonCreditsCast(72129);
        assertNotNull(actorMovs);
        assertEquals("Garden Party", actorMovs[1].getTitle());
        assertEquals(1, count[0]); // 1 request
    }

    @Test
    public void testSearchMovieThenActorsThenMoviesAgain() {
        final RateLimiter rateLimiter = RateLimiter.create(3.0);
        final int[] count = {0};
        IRequest req = new HttpRequest()
                .compose(__ -> count[0]++)
                .compose(System.out::println)
                .compose(__ -> rateLimiter.acquire());

        MovService movapi = new MovService(new MovWebApi(req));

        Iterable<SearchItem> vs = movapi.search("hulk");
        assertEquals(25, Queries.count(vs));// number of returned movies
        assertEquals(3, count[0]);         // 2 requests to consume all pages
        /**
         * Iterable<SearchItem> is Lazy and without cache.
         */
        SearchItem hulk = filter(
                m -> m.getTitle().equals("Shamelessly She-Hulk"),
                vs)
                .iterator()
                .next();
        assertEquals(4, count[0]); // 4 because the movie is in the second page
        assertEquals(421831, hulk.getId());
        assertEquals("Shamelessly She-Hulk", hulk.getTitle());
        assertEquals(4, count[0]); // Keep the same number of requests
        /**
         * getDetails() relation SearchItem ---> Movie is Lazy and supported on Supplier<Movie> with Cache
         */
        assertEquals("Shamelessly She-Hulk", hulk.getDetails().getOriginalTitle());
        assertEquals(5, count[0]); // 1 more request to get the Movie
        assertEquals("", hulk.getDetails().getTagline());
        assertEquals(5, count[0]); // NO more request. It is already in cache
        /**
         * getCast() relation Movie --->* CastItem is Lazy and
         * supported on Supplier<List<CastItem>> with Cache
         */
        Iterable<CastItem> hulkCast = hulk.getDetails().getCast();
        assertEquals(6, count[0]); // 1 more request to get the Movie Cast
        assertEquals("Kierstyn Elrod",
                hulkCast.iterator().next().getName());
        assertEquals(6, count[0]); // NO more request. It is already in cache
        assertEquals("John Nania",
                skip(hulkCast, 2).iterator().next().getName());
        assertEquals(6, count[0]); // NO more request. It is already in cache
        /**
         * CastItem ---> Actor is Lazy and with Cache for Person but No cache for actor credits
         */
        CastItem kierstyn = hulk.getDetails().getCast().iterator().next();
        assertEquals(6, count[0]); // NO more request. It is already in cache
        assertNull(kierstyn.getActor().getPlaceOfBirth()); //person page does not have place of birth information.
        assertEquals(7, count[0]); // 1 more request for Actor Person
        assertNull(kierstyn.getActor().getPlaceOfBirth()); //person page does not have place of birth information.
        assertEquals(7, count[0]); // NO more request. It is already in cache
        assertEquals("Shamelessly She-Hulk",
                kierstyn.getActor().getMovies().iterator().next().getTitle());
        assertEquals(8, count[0]); // 1 more request for Actor Credits
        assertEquals("Shamelessly She-Hulk",
                kierstyn.getActor().getMovies().iterator().next().getTitle());
        assertEquals(9, count[0]); // 1 more request. Actor Cast is not in cache

        /**
         * Check Cache from the beginning
         */
        assertNull(movapi.getMovie(421831).getCast().iterator().next().getActor().getPlaceOfBirth());
        assertEquals(9, count[0]); // No more requests for the same getMovie.
        /*
         * Now get a new Film
         */
        assertEquals("Predator",
                movapi.getMovie(861).getCast().iterator().next().getActor().getMovies().iterator().next().getTitle()); //FIXME: supplier
        assertEquals(12, count[0]); // 1 request for Movie + 1 for CastItems + 1 Person + 1 Actor Credits
    }

    @Test
    public void testSearchMovieWithManyPages() {
        final RateLimiter rateLimiter = RateLimiter.create(3.0);
        final int[] count = {0};
        IRequest req = new HttpRequest()
                .compose(__ -> count[0]++)
                .compose(System.out::println)
                .compose(__ -> rateLimiter.acquire());

        MovService movapi = new MovService(new MovWebApi(req));

        Iterable<SearchItem> vs = movapi.search("fire");
        assertEquals(1166, Queries.count(vs)); // number of returned movies FIXME
        assertEquals(59, count[0]);         // 2 requests to consume all pages FIXME
    }
}
