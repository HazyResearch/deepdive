###
   To define your AngularJS extensions for the custom templates in Mindbender,
   your own JavaScript/CoffeeScript code can be put at mindbender/extensions.js
   or mindbender/extensions.coffee file under the working directory where you
   start `mindbender gui`.  All files under the mindbender/ directory will be
   available under the same relative path, e.g., an AngularJS template stored
   in mindbender/foo.html will be available as "mindbender/foo.html".  Hence,
   you can use arbitrary number of files to define your extensions.
###

angular.module "mindbender.extensions", [
    "mindbender.search"
]

.directive "mbSearchLink", ($location, DeepDiveSearch) ->
    link: ($scope, $element, $attrs) ->
        $scope.$watch (-> $attrs.mbSearchLink), ->
            params = _.extend {}, DeepDiveSearch.params
            params.s = $attrs.mbSearchLink
            params.t = $attrs.mbSearchOnly
            params.p = 1
            $element.attr "href", "#/search/#{
                unless DeepDiveSearch.index? then ""
                else encodeURIComponent DeepDiveSearch.index}?#{(
                "#{encodeURIComponent k}=#{encodeURIComponent v ? ""}" for k,v of params
            ).join("&")}"

.directive "mbView", (DeepDiveSearch) ->
    scope:
        mbView: "="
        mbViewType: "@"
    link: ($scope, $element, $attrs) ->
        # TODO use hit and https://github.com/HazyResearch/mindbender/blob/master/gui/frontend/src/search/search.html#L92-L96
        $element.attr "href", "#/view/#{encodeURIComponent DeepDiveSearch.index}/#{
            encodeURIComponent $scope.mbViewType}/?id=#{
                encodeURIComponent $scope.mbView}#{
                    unless DeepDiveSearch.routing? then ""
                    else "&routing=#{DeepDiveSearch.routing}"
                }"



.filter "uri", () ->
    (s) -> encodeURIComponent s

