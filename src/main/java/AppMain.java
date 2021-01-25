import com.twelvemonkeys.imageio.plugins.psd.PSDImageReader;
import com.twelvemonkeys.imageio.plugins.psd.PSDMetadata;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AppMain {
    private List<String> scanAttributes(Node node) {
        List<String> list = new ArrayList<>();
        NamedNodeMap nodeMap = node.getAttributes();

        for (int ix = 0, len = nodeMap.getLength(); ix < len; ix++) {
            Node item = nodeMap.item(ix);

            list.add(String.format("%s:%s", item.getNodeName(), item.getNodeValue()));
        }
        return list;
    }

    private List<NodeInfo> scanNodeList(NodeList nodeList, int depth) {
        List<NodeInfo> list = new ArrayList<>();

        for (int ix = 0, len = nodeList.getLength(); ix < len; ix++) {
            Node node = nodeList.item(ix);

            if (node instanceof NodeList) {
                list.add(new NodeInfo(depth, node));
                list.addAll(scanNodeList((NodeList) node, depth + 1));
            } else {
                list.add(new NodeInfo(depth, node));
            }
        }
        return list;
    }

    private void showNodes(IIOMetadataNode root) {
        List<NodeInfo> list = scanNodeList(root, 0);

        for (NodeInfo nodeInfo : list) {
            Node node = nodeInfo.getNode();
            String attrs = String.join(", ", scanAttributes(node));

            for (int cnt = 0; cnt < nodeInfo.getDepth(); cnt++) {
                System.out.print("\t");
            }
            System.out.format("%d:%s {%s}\n", nodeInfo.getDepth(), node.getNodeName(), attrs);
        }
    }

    private void showLayers(List<IIOMetadataNode> layerList) {
        int depth = 0;

        for (IIOMetadataNode node : layerList) {
            String attrs = String.join(", ", scanAttributes(node));
            String name = node.getAttribute("name");
            int top = Integer.parseInt(node.getAttribute("top"));
            int left = Integer.parseInt(node.getAttribute("left"));
            int bottom = Integer.parseInt(node.getAttribute("bottom"));
            int right = Integer.parseInt(node.getAttribute("right"));
            int width = right - left;
            int height = bottom - top;
            boolean irrelevant = width == 0 && height == 0;
            boolean closeLayer = "</Layer group>".equals(name);

            if (!closeLayer) {
                for (int cnt = 0; cnt < depth; cnt++) {
                    System.out.print("\t");
                }
                System.out.format("(%dx%d) {%s}\n", width, height, attrs);
            }
            if (irrelevant) {
                if (closeLayer) {
                    depth--;
                } else {
                    depth++;
                }
            }
        }
    }

    private void execute(PSDImageReader reader) throws IOException {
        PSDMetadata metadata = (PSDMetadata) reader.getImageMetadata(0);
        String[] formatNames = metadata.getMetadataFormatNames();
        System.out.println("FormatNames: " + String.join(",", formatNames));
        IIOMetadataNode root = (IIOMetadataNode) metadata.getAsTree(formatNames[0]);
        NodeList layerInfo = root.getElementsByTagName("LayerInfo");
        List<IIOMetadataNode> layerList = new ArrayList<>();

//                    Stream.of(formatNames).forEach(n -> showNodes((IIOMetadataNode) metadata.getAsTree(n)));
        for (int ix = 0, len = layerInfo.getLength(); ix < len; ix++) {
            IIOMetadataNode node = (IIOMetadataNode) layerInfo.item(ix);

            layerList.add(node);
            int imageIndex = ix + 1;
            if (imageIndex < len) {
                BufferedImage image = reader.read(imageIndex);

                if (image != null) {
                    String name = node.getAttribute("name");
                    String fileName = String.format("%02d_%s.png", ix, name);
                    int width = image.getWidth();
                    int height = image.getHeight();

                    System.out.format("\t-> [%s]%dx%d\n", name, width, height);
                    fileName = fileName.replace("/", "_");
                    ImageIO.write(image, "png", new File(fileName));
                }
            }
        }
        Collections.reverse(layerList);
        showLayers(layerList);
    }

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
                    execute((PSDImageReader) reader);
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
