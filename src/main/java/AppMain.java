import com.twelvemonkeys.imageio.plugins.psd.PSDImageReader;
import com.twelvemonkeys.imageio.plugins.psd.PSDMetadata;
import org.w3c.dom.NodeList;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AppMain {
    private void execute(List<File> fileList) throws IOException {
        for (File file : fileList) {
            System.out.println("[" + file + "]");
            try (ImageInputStream input = ImageIO.createImageInputStream(file)) {
                for (Iterator<ImageReader> it = ImageIO.getImageReaders(input); it.hasNext(); ) {
                    ImageReader reader = it.next();

                    System.out.println("-> reader: " + reader);
                    if (!(reader instanceof PSDImageReader)) {
                        continue;
                    }
                    reader.setInput(input);
                    PSDImageReader imageReader = (PSDImageReader) reader;
                    PSDMetadata metadata = (PSDMetadata) imageReader.getImageMetadata(0);
                    String[] formatNames = metadata.getMetadataFormatNames();
                    System.out.println("FormatNames: " + String.join(",", formatNames));
                    IIOMetadataNode root = (IIOMetadataNode) metadata.getAsTree(formatNames[0]);
                    NodeList layerInfo = root.getElementsByTagName("LayerInfo");
                    int num = imageReader.getNumImages(true);

                    for (int ix = 0; ix < num; ix++) {
                        BufferedImage layer = imageReader.read(ix);
                        if (layer == null) {
                            continue;
                        }
                        int width = layer.getWidth();
                        int height = layer.getHeight();

                        System.out.println("** " + ix + ": " + width + "x" + height);
                        System.out.println("layer: " + layer);

                        IIOMetadataNode node = (IIOMetadataNode) layerInfo.item(ix);
                        if (node != null) {
                            System.out.println("name: " + node.getAttribute("name"));
                        }
                    }
                }
            }
        }
    }

    public static void main(String... args) throws Exception {
        List<File> fileList = Stream.of(args).map(File::new).filter(File::isFile).collect(Collectors.toList());

        AppMain app = new AppMain();

        app.execute(fileList);
    }
}
