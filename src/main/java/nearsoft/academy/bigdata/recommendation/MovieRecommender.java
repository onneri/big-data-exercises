package nearsoft.academy.bigdata.recommendation;

import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.impl.model.file.FileDataModel;
import org.apache.mahout.cf.taste.impl.neighborhood.ThresholdUserNeighborhood;
import org.apache.mahout.cf.taste.impl.recommender.GenericUserBasedRecommender;
import org.apache.mahout.cf.taste.impl.similarity.PearsonCorrelationSimilarity;
import org.apache.mahout.cf.taste.model.DataModel;
import org.apache.mahout.cf.taste.neighborhood.UserNeighborhood;
import org.apache.mahout.cf.taste.recommender.RecommendedItem;
import org.apache.mahout.cf.taste.recommender.UserBasedRecommender;
import org.apache.mahout.cf.taste.similarity.UserSimilarity;

import java.io.*;
import java.util.*;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;


public class MovieRecommender {
    private UserBasedRecommender recommender;
    private Map <String,Long> users;
    private Map <String,Long> products;
    private Map <Long,String> productsInverse;
    private Long totalReviews;

    public MovieRecommender(String fileName) throws IOException, TasteException {
        InputStream fileStream = new FileInputStream(fileName);
        InputStream gzipStream = new GZIPInputStream(fileStream);
        Reader decoder = new InputStreamReader(gzipStream);
        BufferedReader buffered = new BufferedReader(decoder);
        String newFileName = fileName.replaceAll("\\..*","")+"Formatted.txt";
        OutputStream formattedFile = new FileOutputStream(newFileName);
        Writer writer = new PrintWriter(formattedFile);
        Pattern p = Pattern.compile(".*: ");
        users = new HashMap<String,Long>();
        products = new HashMap<String, Long>();
        productsInverse = new HashMap<Long, String>();
        DataModel model;
        UserSimilarity similarity;
        UserNeighborhood neighborhood;


        String line;
        String rate;
        Long product,user;
        long sizeUsers=0;
        long sizeProducts=0;

        String[] strings={};
        totalReviews = new Long(0);
        boolean exit=false;
        while (true) {
            do{
                line = buffered.readLine();
                if(line==null){
                    exit=true;
                    break;
                }
                strings = line.split("productId: ");
            }while(strings.length != 2);
            if(exit)
                break;
            product = (Long)products.get(strings[1]);
            if(product==null){
                products.put(strings[1],product=++sizeProducts);
                productsInverse.put(product,strings[1]);
            }
            do{
                line = buffered.readLine();
                if(line==null){
                    exit=true;
                    break;
                }
                strings = line.split("userId: ");
            }while(strings.length != 2);
            if(exit)
                break;
            user = (Long) users.get(strings[1]);
            if (user == null) {
                users.put(strings[1], user = ++sizeUsers);
            }
            do{
                line = buffered.readLine();
                if(line==null){
                    exit=true;
                    break;
                }
                strings = line.split("score: ");
            }while(strings.length != 2) ;
            if(exit)
                break;
                rate = strings[1];
                writer.write(user + "," + product + "," + rate + "\n");
                totalReviews++;
        }
        writer.close();
        buffered.close();
        decoder.close();
        gzipStream.close();
        fileStream.close();

        model = new FileDataModel(new File(newFileName));
        similarity = new PearsonCorrelationSimilarity(model);
        neighborhood = new ThresholdUserNeighborhood(.1, similarity, model);
        recommender = new GenericUserBasedRecommender(model, neighborhood, similarity);
    }

    public long getTotalReviews(){
        return totalReviews;
    }
    public long getTotalProducts(){
        return products.size();
    }

    public int getTotalUsers() {
        return users.size();
    }

    public List<String> getRecommendationsForUser(String userId) throws TasteException {
        List<String> listRecommendations = new ArrayList<String>();
        List<RecommendedItem> recommendations = recommender.recommend(users.get(userId), 3);

        for(RecommendedItem recommendation:recommendations){
            listRecommendations.add(productsInverse.get(recommendation.getItemID()));
        }
        return listRecommendations;
    }
}
