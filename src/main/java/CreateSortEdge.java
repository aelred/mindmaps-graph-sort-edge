import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import io.mindmaps.graph.config.MindmapsGraphFactory;
import org.apache.tinkerpop.gremlin.process.traversal.Order;
import org.apache.tinkerpop.gremlin.structure.Direction;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Vertex;

import java.util.Comparator;
import java.util.List;

public class CreateSortEdge {
    private static Graph mainGraph;


    public static void main(String [] args){
        Logger logger = (Logger) org.slf4j.LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        logger.setLevel(Level.OFF);

        if(args.length < 1){
            System.out.println("Parameters missing. First parameter must specify the titan cassandra file.");
            return;
        }

        System.out.println("Creating connection to graph . . . ");
        mainGraph = MindmapsGraphFactory.buildNewTransaction(args[0]);
        System.out.println();
        System.out.println("Querying genres . . .");
        List<Vertex> genres = mainGraph.traversal().V().has("ITEM_IDENTIFIER", "http://mindmaps.io/genre").in("ISA").order().by("DEGREE", Order.decr).toList();

        System.out.println("Creating edges");
        createdNextEdges(genres);

        mainGraph.tx().commit();
        try {
            mainGraph.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("Done");
    }

    private static void createdNextEdges(List<Vertex> vertexes){
        Vertex currentVertex = vertexes.get(0);
        for(int i =1; i < vertexes.size(); i ++ ){
            Vertex nextVertex = vertexes.get(i);

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
