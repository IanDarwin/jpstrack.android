GENERATED = index.html privacy.html RELEASE_NOTES.html
install:	$(GENERATED)
	cp -r * /var/www/htdocs/darwinsys.com/jpstrack
index.html:	index.adoc
	asciidoctor index.adoc
privacy.html:	privacy.adoc
	asciidoctor privacy.adoc
RELEASE_NOTES.html:	RELEASE_NOTES.adoc
	asciidoctor RELEASE_NOTES.adoc
