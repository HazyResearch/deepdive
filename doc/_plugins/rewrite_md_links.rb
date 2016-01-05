# A plugin to rewrite GitHub-friendly .md links to ones without the extension for the website
# See: http://stackoverflow.com/a/29234076
Jekyll::Hooks.register :pages, :pre_render do |page|
    page.content = page.content.gsub(/(\[[^\]]*\]\([^:\)]*)\.md(#.*)?\)/, '\1\2)')
end
