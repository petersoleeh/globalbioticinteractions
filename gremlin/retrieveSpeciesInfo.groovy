
def lookupInfo(String speciesName) {
    linkedData = new LinkedDataSailGraph(new MemoryStoreSailGraph())
    v = linkedData.v('http://dbpedia.org/resource/' + speciesName.replace(' ', '_'))
    ont = []
    for (c in ['kingdom', 'phylum', 'class', 'order', 'family', 'genus', 'species', 'thumbnail']) {
        ont += 'http://dbpedia.org/ontology/' + c
    }
    redirect = 'http://dbpedia.org/ontology/wikiPageRedirects'
    info = []

    v.outE.filter {ont.contains(it.label)}.uniqueObject.aggregate(info)
    v.outE.filter {redirect == it.label}.inV.outE.filter {ont.contains(it.label)}.uniqueObject.aggregate(info)
    linkedData.shutdown()
    info += v
    info
}

lookupInfo('Micropogonias undulatus')
lookupInfo('Polydactylus octonemus')
