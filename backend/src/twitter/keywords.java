package twitter;
public class keywords {
    public String keyword(String text) {
        String result = "";
        if (text.toLowerCase().contains("newyork")|| text.toLowerCase().contains("new york")) {
            result = "newyork";
        } else if (text.toLowerCase().contains("university")) {
            result = "university";
        } else if (text.toLowerCase().contains("card")) {
            result = "card";
        } else if (text.toLowerCase().contains("nba")) {
            result = "nba";
        } else if (text.toLowerCase().contains("tennis")) {
            result = "tennis";
        } else if (text.toLowerCase().contains("apple")) {
            result = "apple";
        }
        else {
            result = null;
        }
        return result;
    }

}
