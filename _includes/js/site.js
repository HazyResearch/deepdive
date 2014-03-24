if (window.location.href.indexOf('http://dennybritz.github.io') === 0) {
  window.location.href = 'http://deepdive.stanford.edu/';
}

$(function(){
  analytics.trackLink($("a[href='https://github.com/hazyresearch/deepdive/archive/master.zip']"), "click_github_download");
})