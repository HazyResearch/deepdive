---
---

{% include js/jquery-1.10.1.min.js %}
{% include js/bootstrap.min.js %}
{% include js/toc.js %}

$(function(){
    /* prevent bash code blocks with multiple lines from triggering a CSS style that adds a prompt prefix */
    $("code.language-bash")
        .filter(function(){ return $(this).text().trim().indexOf("\n") > -1; })
        .addClass("multi-line");
});
