<%-- 
    Document   : dashboard
    Author     : Stephen Xie
--%>
<%-- add the line below, or JSTL won't work --%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>Hacker News Relay API Server Dashboard</title>
        <link rel="stylesheet" type="text/css" href="css/global.css">
    </head>
    <body>
        <h1>Hacker News Relay API Server Dashboard</h1>
        <h2>Operations Analytics</h2>
        <p><b>Average Query Latency:</b> <%
            Double avgQueryLatency = (Double) request.getAttribute("avg_q_time");
            if (avgQueryLatency != null) {
                out.println(String.format("%.4f seconds", avgQueryLatency));
            } else {
                out.println("Not available");
            }
        %></p>
        <p><b>Most Queried Story:</b> <%
            String mostFreqID = (String) request.getAttribute("most_freq_id");
            String mostFreqTitle = (String) request.getAttribute("most_freq_title");
            String mostFreqCount = (String) request.getAttribute("most_freq_cont");
            if (mostFreqID != null && mostFreqTitle != null) {
                out.println(String.format("%s (id: %s; queried %s times)", mostFreqTitle, mostFreqID, mostFreqCount));
            } else {
                out.println("Not available");
            }
        %></p>
        <p><b>Average HN API Service Downtime:</b> <%
            out.println((Double)request.getAttribute("hn_down_freq") + " per month");
        %></p>
        <hr>
        
        <h2>Full Logs</h2>
        <h3>All recorded stories queries:</h3>
        <c:choose>
            <c:when test="${story_queried != null}">
                <table>
                    <tr>
                        <th>ID</th>
                        <th>Title</th>
                        <th>Date</th>
                    </tr>
                    <c:forEach items="${story_queried}" var="item">
                        <tr>
                            <c:forEach items="${item}" var="sub">
                                <td>${sub}</td>
                            </c:forEach>
                        </tr>
                    </c:forEach>
                </table>
            </c:when>
            <c:otherwise>
                <p>No record found.</p>
            </c:otherwise>
        </c:choose>
        
        <h3>All recorded API down times:</h3>
        <c:choose>
            <c:when test="${api_down != null}">
                <table>
                    <tr>
                        <th>Date</th>
                    </tr>
                    <c:forEach items="${api_down}" var="item">
                        <tr>${item}</tr>
                    </c:forEach>
                </table>
            </c:when>
            <c:otherwise>
                <p>No record found.</p>
            </c:otherwise>
        </c:choose>
        
        <h3>All recorded malformed requests:</h3>
        <c:choose>
            <c:when test="${wrong_reqs != null}">
                <table>
                    <tr>
                        <th>Type</th>
                        <th>Date</th>
                        <th>Info</th>
                    </tr>
                    <c:forEach items="${wrong_reqs}" var="item">
                        <tr>
                            <c:forEach items="${item}" var="sub">
                                <td>${sub}</td>
                            </c:forEach>
                        </tr>
                    </c:forEach>
                </table>
            </c:when>
            <c:otherwise>
                <p>No record found.</p>
            </c:otherwise>
        </c:choose>
                
        <h3>All recorded query latency:</h3>
        <c:choose>
            <c:when test="${api_latency != null}">
                <table>
                    <tr>
                        <th>Type</th>
                        <th>Time (ms)</th>
                    </tr>
                    <c:forEach items="${api_latency}" var="item">
                        <tr>
                            <c:forEach items="${item}" var="sub">
                                <td>${sub}</td>
                            </c:forEach>
                        </tr>
                    </c:forEach>
                </table>
            </c:when>
            <c:otherwise>
                <p>No record found.</p>
            </c:otherwise>
        </c:choose>
                
        <br>
        <footer>&COPY; 2017 Stephen Xie</footer>
    </body>
</html>
