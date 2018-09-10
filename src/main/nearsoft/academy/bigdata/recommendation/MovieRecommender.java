package nearsoft.academy.bigdata.recommendation;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;
import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.impl.model.file.FileDataModel;
import org.apache.mahout.cf.taste.impl.neighborhood.ThresholdUserNeighborhood;
import org.apache.mahout.cf.taste.impl.recommender.GenericUserBasedRecommender;
import org.apache.mahout.cf.taste.impl.similarity.PearsonCorrelationSimilarity;
import org.apache.mahout.cf.taste.model.DataModel;
import org.apache.mahout.cf.taste.neighborhood.UserNeighborhood;
import org.apache.mahout.cf.taste.recommender.RecommendedItem;
import org.apache.mahout.cf.taste.similarity.UserSimilarity;


public class MovieRecommender {

  private final GenericUserBasedRecommender recommender;
  HashMap<String, Integer> usersMap = new HashMap<>();
  BidiMap<String, Integer> productsMap = new DualHashBidiMap<>();

  int numberOfReviews;

  public MovieRecommender(String path) throws IOException, TasteException {
    DataModel model = new FileDataModel(new File(getCsvFromTextFile(path)));
    UserSimilarity similarity = new PearsonCorrelationSimilarity(model);
    UserNeighborhood neighborhood = new ThresholdUserNeighborhood(0.1, similarity, model);
    recommender = new GenericUserBasedRecommender(model, neighborhood, similarity);
  }

  public String getCsvFromTextFile(String path) throws IOException {
    InputStream is = new FileInputStream(new File(path));
    BufferedReader br = new BufferedReader(new InputStreamReader(is));

    File csv = new File("output.csv");
    if (csv.exists()) {
      csv.delete();
    } else {
      csv.createNewFile();
    }
    Writer writer = new BufferedWriter(new FileWriter(csv));

    String st;
    int userId = -1;
    int productId = -1;
    double score = -1;
    boolean finishedReadingReview = false;

    while ((st = br.readLine()) != null) {
      if (st.length() != 0) {
        if (st.contains(":")) {
          String[] line = st.split(":");
          String key = line[0].trim();
          String value = line[1].trim();
          if (key.contains("productId")) {
            if (!productsMap.containsKey(value)) {
              productsMap.put(value, productsMap.size() + 1);
            }
            productId = productsMap.get(value);
            numberOfReviews++;
          } else if (key.contains("userId")) {
            if (!usersMap.containsKey(value)) {
              usersMap.put(value, usersMap.size() + 1);
            }
            userId = usersMap.get(value);
          } else if (key.contains("score")) {
            finishedReadingReview = true;
            score = Double.parseDouble(value);
          }
          if (finishedReadingReview) {
            writer.append(userId + ",");
            writer.append(productId + ",");
            writer.append(score + "\n");
            userId = -1;
            productId = -1;
            score = -1;
            finishedReadingReview = false;
          }
        }
      }
    }
    writer.close();
    is.close();
    br.close();
    return csv.getAbsolutePath();
  }

  public int getTotalReviews() {
    return numberOfReviews;
  }

  public int getTotalProducts() {
    return productsMap.size();
  }

  public int getTotalUsers() {
    return usersMap.size();
  }


  public List<String> getRecommendationsForUser(String userId) throws TasteException {
    List<RecommendedItem> recommendations = recommender.recommend(usersMap.get(userId), 3);
    List<String> recommendedMovies = new ArrayList<>();
    BidiMap<Integer, String> rMap = productsMap.inverseBidiMap();

    for (RecommendedItem recommendation : recommendations) {
      String movieName = rMap.get((int) recommendation.getItemID());
      recommendedMovies.add(movieName);
    }
    return recommendedMovies;
  }
}