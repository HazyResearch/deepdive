# A plugin to rewrite GitHub-friendly .md links to ones without the extension for the website
begin
    # jekyll >= 3.x
    Jekyll::Hooks.register :pages, :pre_render do |page|
        page.content = page.content.gsub(/(\[[^\]]*\]\([^:\)]*)\.md(#.*)?\)/, '\1\2)')
    end
rescue
    # jekyll <= 2.x
    # See: http://stackoverflow.com/a/29234076
    module ChangeLocalMdLinksToHtml
        class Generator < Jekyll::Generator
            def generate(site)
                site.pages.each { |p| rewrite_links(site, p) }
            end
            def rewrite_links(site, page)
                page.content = page.content.gsub(/(\[[^\]]*\]\([^:\)]*)\.md(#.*)?\)/, '\1\2)')
            end
        end
    end
end
