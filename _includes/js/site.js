$(function(){
  $("a[href='https://github.com/dennybritz/deepdive/archive/master.zip']").on("click", function(){
    analytics.track("click_github_download")
    
  });
})