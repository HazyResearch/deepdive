---
---

{% include js/jquery-1.10.1.min.js %}
{% include js/bootstrap.min.js %}
{% include js/toc.js %}

$(function(){
    /* prevent bash code blocks with multiple lines from triggering a CSS style that adds a prompt prefix */
    $("code.language-bash")
        .filter(function(){
            var text = $(this).text().trim();
            return (
                // multiple lines
                text.indexOf("\n") > -1 &&
                // with some lines not beginning with whitespace
                ! text.split("\n").slice(1).every(function(l){ return /^\s|^$/.test(l); })
            );
        })
        .addClass("multi-commands");
});
