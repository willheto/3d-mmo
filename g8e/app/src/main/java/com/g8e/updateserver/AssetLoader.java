package com.g8e.updateserver;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AssetLoader {

    public List<Asset> getAssets(String directoryPath) throws IOException, URISyntaxException {
        URL resourceUrl = getClass().getResource(directoryPath);
        if (resourceUrl == null) {
            throw new IllegalArgumentException("Resource not found: " + directoryPath);
        }

        URI uri = resourceUrl.toURI();
        List<Asset> assets = new ArrayList<>();

        if ("jar".equals(uri.getScheme())) {
            try (FileSystem fs = FileSystems.newFileSystem(uri, Map.of())) {
                Path root = fs.getPath(directoryPath);
                loadDirectory(root, assets);
            }
        } else {
            Path root = Paths.get(uri);
            loadDirectory(root, assets);
        }

        return assets;
    }

    private void loadDirectory(Path dir, List<Asset> assets) throws IOException {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            for (Path entry : stream) {
                if (Files.isDirectory(entry)) {
                    List<Asset> children = new ArrayList<>();
                    loadDirectory(entry, children);
                    assets.add(new Asset(
                            entry.getFileName().toString(),
                            "directory",
                            children));
                } else {
                    assets.add(new Asset(
                            entry.getFileName().toString(),
                            "file",
                            Files.readAllBytes(entry)));
                }
            }
        }
    }

    public static class Asset {
        private final String name;
        private final String type;
        private final Object data;

        public Asset(String name, String type, Object data) {
            this.name = name;
            this.type = type;
            this.data = data;
        }
    }
}
