package hackernews.api.server;

import java.io.IOException;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Controller of the server dashboard.
 * 
 * @author Stephen Xie &lt;[redacted]@andrew.cmu.edu&gt;
 */
@WebServlet(name = "DashboardServlet", urlPatterns = {"/dashboard"})
public class DashboardServlet extends HttpServlet {
    
    private APIUsageLogger logger;  // logger to the remote MongoDB database

    @Override
    public void init() throws ServletException {
        super.init();
        
        // initialize the logger service
        logger = APIUsageLogger.getInstance();
    }

    /**
     * Processes requests for HTTP <code>GET</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // analytics: average latency of making queries to the HackerNews API
        double averageQueryLatency = logger.getAverageQueryLatency();  // in seconds
        if (averageQueryLatency > 0)
            request.setAttribute("avg_q_time", averageQueryLatency);
        
        // analytics: ID and the title of the most queried story
        String[] mostFreqStory = logger.getMostQueriedStory();
        if (mostFreqStory != null) {
            request.setAttribute("most_freq_id", mostFreqStory[0]);
            request.setAttribute("most_freq_title", mostFreqStory[1]);
            request.setAttribute("most_freq_cont", mostFreqStory[2]);
        }
        
        // analytics: month-based frequency of HackerNews API service going down
        double hnServiceDownFreq = logger.getHNApiDownFreq();
        request.setAttribute("hn_down_freq", hnServiceDownFreq);
        
        // get logs
        request.setAttribute("api_latency", logger.getAllHNAPIQueryLatency());
        request.setAttribute("story_queried", logger.getAllStoryQueried());
        request.setAttribute("api_down", logger.getAllHNApiDownTime());
        request.setAttribute("wrong_reqs", logger.getAllMalformedRequest());
        
        // finally, direct to the dashboard view with the necessary parameters
        RequestDispatcher view = request.getRequestDispatcher("dashboard.jsp");
        view.forward(request, response);
    }

    
    /**
     * Handles the HTTP <code>GET</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Handles the HTTP <code>POST</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setStatus(405);  // method not allowed
    }

    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Short description";
    }

}
