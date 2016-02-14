# A plugin to rewrite GitHub-friendly .md links to ones without the extension for the website
begin
    # jekyll >= 3.x
    Jekyll::Hooks.register :pages, :pre_render, priority: :lowest do |page|
        # manipulate Markdown links to drop .md extensions
        #STDERR.puts "rewrite_md_links.rb: pre_render #{page.url}" if page.content.match(/(\[[^\]]*\]\([^:\)]*)\.md(#[^\)]*)?\)/)
        page.content = page.content.gsub(/(\[[^\]]*\]\([^:\)]*)\.md(#[^\)]*)?\)/, '\1\2)')
    end
    Jekyll::Hooks.register :pages, :post_render do |page|
        # need to manipulate HTML links as well because Markdown from {% include ... %} don't go through :pre_render
        #STDERR.puts "rewrite_md_links.rb: post_render #{page.url}" if page.output.match(/href="([^":]+)\.md(#[^"]*)?"/)
        page.output = page.output.gsub(/href="([^":]+)\.md(#[^"]*)?"/, 'href="\1\2"')
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
                page.content = page.content.gsub(/(\[[^\]]*\]\([^:\)]*)\.md(#[^)]*)?\)/, '\1\2)')
            end
        end
    end
end
