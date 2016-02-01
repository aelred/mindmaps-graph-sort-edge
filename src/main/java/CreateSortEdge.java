import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import io.mindmaps.graph.config.MindmapsGraphFactory;
import org.apache.tinkerpop.gremlin.process.traversal.Order;
import org.apache.tinkerpop.gremlin.structure.Direction;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Vertex;

import java.util.List;
import java.util.stream.Stream;

public class CreateSortEdge {
    private static final String[] TYPES = new String[] {
            "http://mindmaps.io/genre",
            "http://mindmaps.io/award",
            "http://mindmaps.io/mood",
            "http://mindmaps.io/location",
            "http://mindmaps.io/language",
            "http://mindmaps.io/person",
            "http://mindmaps.io/character",
            "http://mindmaps.io/company",
            "http://mindmaps.io/certificate",
            "http://mindmaps.io/keyword"
    };


    public static void main(String [] args){
        Logger logger = (Logger) org.slf4j.LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        logger.setLevel(Level.OFF);

        if(args.length < 1){
            System.out.println("Parameters missing. First parameter must specify the titan cassandra file.");
            return;
        }

        Stream.of(TYPES).parallel().forEach(type -> createNextEdgesForType(args[0], type));

        System.out.println("Done");
    }

    private static void createNextEdgesForType(String config, String typeId) {
        System.out.println(typeId);

        Graph graph = MindmapsGraphFactory.buildNewTransaction(config);

        List<Vertex> vertices = graph.traversal().V()
                .has("ITEM_IDENTIFIER", typeId)
                .in("ISA")
                .order()
                .by("DEGREE", Order.decr)
                .toList();

        System.out.println("Creating edges: " + typeId);
        createNextEdges(vertices);

        System.out.println("top and bottom: " + typeId);

        Vertex typeVertex = graph.traversal().V().has("ITEM_IDENTIFIER", typeId).next();

        if (vertices.size() > 0) {
            if (!typeVertex.edges(Direction.OUT, "TOP").hasNext()) typeVertex.addEdge("TOP", vertices.get(0));
            if (!typeVertex.edges(Direction.OUT, "BOTTOM").hasNext())
                typeVertex.addEdge("BOTTOM", vertices.get(vertices.size() - 1));
        }

        graph.tx().commit();
        try {
            graph.close();
        } catch (Exception e) {
            e.printStackTrace();
            createNextEdgesForType(config, typeId);
        }

        System.out.println("Done: " + typeId);
    }

    private static void createNextEdges(List<Vertex> vertices){
        if (vertices.size() < 2) return;

        Vertex currentVertex = vertices.get(0);
        for(int i =1; i < vertices.size(); i ++ ){
            Vertex nextVertex = vertices.get(i);

            //Check if edge exists
            if(currentVertex.edges(Direction.OUT, "NEXT").hasNext()){
                System.out.println("NEXT edge already exists between [" + currentVertex + "] and [" + nextVertex + "]");
            } else {
                System.out.println("Creating Edge between [" + currentVertex + "] and [" + nextVertex + "]");
                currentVertex.addEdge("NEXT", nextVertex);
            }

            currentVertex = nextVertex;
        }
    }
}
