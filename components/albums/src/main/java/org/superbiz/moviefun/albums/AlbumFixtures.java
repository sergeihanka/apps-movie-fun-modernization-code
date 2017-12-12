package org.superbiz.moviefun.albums;

import org.springframework.stereotype.Component;

import java.util.List;

import static java.util.Arrays.asList;

@Component
public class AlbumFixtures {

    public List<org.superbiz.moviefun.albums.Album> load() {
        return asList(
            new org.superbiz.moviefun.albums.Album("Massive Attack", "Mezzanine", 1998, 9),
            new org.superbiz.moviefun.albums.Album("Radiohead", "OK Computer", 1997, 8),
            new org.superbiz.moviefun.albums.Album("Radiohead", "Kid A", 2000, 9)
        );
    }
}
