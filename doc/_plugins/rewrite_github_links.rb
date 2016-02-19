# A plugin to rewrite GitHub-friendly links with relative paths to absolute URLs
GITHUB_REPO_BASE_URL = "https://github.com/HazyResearch/deepdive/blob/master"
def rewrite_markdown_links(page)
    # manipulate Markdown links to map ../examples/* to GitHub repo
    page.content = page.content.gsub(
        /(\[[^\]]*\]\()(\.\.\/)+((examples|compiler|database|inference|runner|shell|util|ddlib|test)\/[^:\)]*)(#[^\)]*)?\)/,
            "\\1#{GITHUB_REPO_BASE_URL}/\\3\\5)")
end
def rewrite_html_links(page)
    # manipulate HTML links as well to map ../examples/* to GitHub repo
    # because Markdown from {% include ... %} don't go through :pre_render
    page.output = page.output.gsub(
        /href="(\.\.\/)+((examples|compiler|database|inference|runner|shell|util|ddlib|test)\/[^":]*)(#[^"]*)?"/,
            "href=\"#{GITHUB_REPO_BASE_URL}/\\2\\4\"")
end

begin
    # jekyll >= 3.x
    Jekyll::Hooks.register :pages, :pre_render, priority: :lowest do |page|
        rewrite_markdown_links(page)
    end
    Jekyll::Hooks.register :pages, :post_render do |page|
        rewrite_html_links(page)
    end
rescue
    # jekyll <= 2.x
    # See: http://stackoverflow.com/a/29234076
    module ChangeLocalMdLinksToHtml
        class Generator < Jekyll::Generator
            def generate(site)
                site.pages.each rewrite_markdown_links
            end
        end
    end
end
