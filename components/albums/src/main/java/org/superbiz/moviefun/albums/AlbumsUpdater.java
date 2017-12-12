package org.superbiz.moviefun.albums;

import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.superbiz.moviefun.blobstore.Blob;
import org.superbiz.moviefun.blobstore.BlobStore;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static com.fasterxml.jackson.dataformat.csv.CsvSchema.ColumnType.NUMBER;
import static org.superbiz.moviefun.CsvUtils.readFromCsv;
//import static org.superbiz.moviefun.CsvUtils.readFromCsv;

@Service
public class AlbumsUpdater {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final ObjectReader objectReader;
    private final BlobStore blobStore;
    private final org.superbiz.moviefun.albums.AlbumsBean albumsBean;

    public AlbumsUpdater(BlobStore blobStore, org.superbiz.moviefun.albums.AlbumsBean albumsBean) {
        this.blobStore = blobStore;
        this.albumsBean = albumsBean;

        CsvSchema schema = CsvSchema.builder()
            .addColumn("artist")
            .addColumn("title")
            .addColumn("year", NUMBER)
            .addColumn("rating", NUMBER)
            .build();

        objectReader = new CsvMapper().readerFor(org.superbiz.moviefun.albums.Album.class).with(schema);
    }

    public void update() throws IOException {
        Optional<Blob> maybeBlob = blobStore.get("albums.csv");

        if (!maybeBlob.isPresent()) {
            logger.info("No albums.csv found when running AlbumsUpdater!");
            return;
        }

        List<org.superbiz.moviefun.albums.Album> albumsToHave = readFromCsv(objectReader, maybeBlob.get().inputStream);
        List<org.superbiz.moviefun.albums.Album> albumsWeHave = albumsBean.getAlbums();

        createNewAlbums(albumsToHave, albumsWeHave);
        deleteOldAlbums(albumsToHave, albumsWeHave);
        updateExistingAlbums(albumsToHave, albumsWeHave);
    }


    private void createNewAlbums(List<org.superbiz.moviefun.albums.Album> albumsToHave, List<org.superbiz.moviefun.albums.Album> albumsWeHave) {
        Stream<org.superbiz.moviefun.albums.Album> albumsToCreate = albumsToHave
            .stream()
            .filter(album -> albumsWeHave.stream().noneMatch(album::isEquivalent));

        albumsToCreate.forEach(albumsBean::addAlbum);
    }

    private void deleteOldAlbums(List<org.superbiz.moviefun.albums.Album> albumsToHave, List<org.superbiz.moviefun.albums.Album> albumsWeHave) {
        Stream<org.superbiz.moviefun.albums.Album> albumsToDelete = albumsWeHave
            .stream()
            .filter(album -> albumsToHave.stream().noneMatch(album::isEquivalent));

        albumsToDelete.forEach(albumsBean::deleteAlbum);
    }

    private void updateExistingAlbums(List<org.superbiz.moviefun.albums.Album> albumsToHave, List<org.superbiz.moviefun.albums.Album> albumsWeHave) {
        Stream<org.superbiz.moviefun.albums.Album> albumsToUpdate = albumsToHave
            .stream()
            .map(album -> addIdToAlbumIfExists(albumsWeHave, album))
            .filter(org.superbiz.moviefun.albums.Album::hasId);

        albumsToUpdate.forEach(albumsBean::updateAlbum);
    }

    private org.superbiz.moviefun.albums.Album addIdToAlbumIfExists(List<org.superbiz.moviefun.albums.Album> existingAlbums, org.superbiz.moviefun.albums.Album album) {
        Optional<org.superbiz.moviefun.albums.Album> maybeExisting = existingAlbums.stream().filter(album::isEquivalent).findFirst();
        maybeExisting.ifPresent(existing -> album.setId(existing.getId()));
        return album;
    }
}
