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

public class MovServiceTestForHulk {

    @Test
    public void testSearchMovieInSinglePage() {
        MovService movapi = new MovService(new MovWebApi(new HttpRequest()));
        Iterable<SearchItem> movs = movapi.search("Deadpool");
        SearchItem m = movs.iterator().next();
        assertEquals("Deadpool", m.getTitle());
        assertEquals(7, Queries.count(movs));
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
        filter(
                m -> m.getTitle().equals("Deadly Games"),
                movs)
                .iterator()
                .next();
        assertEquals(3, count[0]); // Found on 3rd page
        assertEquals(291, Queries.count(movs));
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
        assertEquals(1, count[0]);
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
        assertEquals(25, Queries.count(vs));
        assertEquals(3, count[0]);

        SearchItem hulk = filter(
                m -> m.getTitle().equals("Shamelessly She-Hulk"),
                vs)
                .iterator()
                .next();
        assertEquals(4, count[0]); // 4 because the movie is in the second page
        assertEquals(421831, hulk.getId());
        assertEquals("Shamelessly She-Hulk", hulk.getTitle());
        assertEquals(4, count[0]);

        assertEquals("Shamelessly She-Hulk", hulk.getDetails().getOriginalTitle());
        assertEquals(5, count[0]);
        assertEquals("", hulk.getDetails().getTagline());
        assertEquals(5, count[0]);

        Iterable<CastItem> hulkCast = hulk.getDetails().getCast();
        assertEquals(6, count[0]);
        assertEquals("Kierstyn Elrod",
                hulkCast.iterator().next().getName());
        assertEquals(6, count[0]);
        assertEquals("John Nania",
                skip(hulkCast, 2).iterator().next().getName());
        assertEquals(6, count[0]);

        CastItem kierstyn = hulk.getDetails().getCast().iterator().next();
        assertEquals(6, count[0]);
        assertNull(kierstyn.getActor().getPlaceOfBirth()); //person page does not have that information
        assertEquals(7, count[0]);
        assertNull(kierstyn.getActor().getPlaceOfBirth()); //person page does not have that information
        assertEquals(7, count[0]);
        assertEquals("Shamelessly She-Hulk",
                kierstyn.getActor().getMovies().iterator().next().getTitle());
        assertEquals(8, count[0]);
        assertEquals("Shamelessly She-Hulk",
                kierstyn.getActor().getMovies().iterator().next().getTitle());
        assertEquals(9, count[0]);

        assertNull(movapi.getMovie(421831).getCast().iterator().next().getActor().getPlaceOfBirth());
        //person page does not have place of birth information
        assertEquals(9, count[0]);

        assertEquals("Reindeer Games",
                movapi.getMovie(2155).getCast().iterator().next().getActor().getMovies().iterator().next().getTitle()); //FIXME: supplier
        assertEquals(13, count[0]);
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

        Iterable<SearchItem> vs = movapi.search("water");
        assertEquals(863, Queries.count(vs));
        assertEquals(45, count[0]);
    }
}
