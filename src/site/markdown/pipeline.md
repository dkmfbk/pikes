Extraction pipeline
===



    # INSTANCE CREATION

    INSERT { GRAPH ?g { ?i a ks:Attribute. }
             ?m ks:expresses ?g; ks:denotes ?i. }
    WHERE  { ?m a ks:AttributeMention; nif:anchorOf ?a; ks:headModifiersSynsetID ?i.
             BIND(rr:mint(fact:, ?m) AS ?g) }

    INSERT { GRAPH ?g { ?i a ks:Instance. }
             ?m ks:expresses ?g; ks:denotes ?i. }
    WHERE  { ?m a ks:InstanceMention; nif:anchorOf ?a.
             FILTER NOT EXISTS { ?m a ks:TimeMention. }
             FILTER NOT EXISTS { ?m a ks:AttributeMention. }
             FILTER NOT EXISTS { ?m a ks:FrameMention; ks:roleset ?rs. ?rs a ks:ArgumentNominalization. }
             BIND(rr:mint(?a, ?m) AS ?i)
             BIND(rr:mint(fact:, ?m) AS ?g) }

    INSERT { GRAPH ?g { ?i a ks:Instance, ks:Time. }
             ?m ks:expresses ?g; ks:denotes ?i. }
    WHERE  { ?m a ks:TimeMention; ks:timeInterval ?i.
             BIND(rr:mint(fact:, ?m) AS ?g) }

    INSERT { GRAPH ?g { ?i a ks:Instance. ?if a ks:Instance, ks:Frame. }
             ?m ks:expresses ?g; ks:denotes ?i; ks:implies ?if. }
    WHERE  { ?m a ks:FrameMention; nif:anchorOf ?a; ks:roleset ?rs.
             ?rs a ks:ArgumentNominalization.
             BIND(rr:mint(?a, ?m) AS ?i)
             BIND(rr:mint(concat(?a, "_pred"), ?m) AS ?if)
             BIND(rr:mint(fact:, ?m) AS ?g) }


    # TYPING

    INSERT { GRAPH ?g { ?i a ?t. } ?m ks:expresses ?g. }
    WHERE  { ?m a ks:InstanceMention; ks:denotes ?i; ks:synset ?s.
             ?s ks:mappedTo ?t.
             BIND(rr:mint(fact:, ?m) AS ?g) }

    INSERT { GRAPH ?g { ?f a ?t. } ?m ks:expresses ?g. }
    WHERE  { ?m a ks:FrameMention; ks:denotes|ks:implies ?f; ks:roleset ?s.
             ?f a ks:Frame. ?s ks:mappedTo ?t.
             BIND(rr:mint(fact:, ?m) AS ?g) }


    # NAMING

    INSERT { GRAPH ?g { ?i rdfs:label ?a. } ?m ks:expresses ?g. }
    WHERE  { ?m a ks:InstanceMention; ks:denotes ?i; nif:anchorOf ?a.
             BIND(rr:mint(fact:, ?m) AS ?g) }

    INSERT { GRAPH ?g { ?i foaf:name ?a. } ?m ks:expresses ?g. }
    WHERE  { ?m a ks:NameMention; ks:denotes ?i; nif:anchorOf ?a.
             BIND(rr:mint(fact:, ?m) AS ?g) }


    # DBPEDIA LINKING

    INSERT { GRAPH ?g { ?i owl:sameAs ?u. } ?m ks:expresses ?g. }
    WHERE  { ?m a ks:NameMention; ks:denotes ?i; ks:linkedTo ?u.
             BIND(rr:mint(fact:, ?m) AS ?g) }

    INSERT { GRAPH ?g { ?i rdfs:seeAlso ?u. } ?m ks:expresses ?g. }
    WHERE  { ?m a ks:InstanceMention; ks:denotes ?i; ks:linkedTo ?u.
             FILTER NOT EXISTS { ?m a ks:NameMention. }
             BIND(rr:mint(fact:, ?m) AS ?g) }


    # PARTICIPATION

    INSERT { GRAPH ?g { ?if ?p ?ia. } ?m ks:expresses ?g. }
    WHERE  { ?m a ks:ParticipationMention; ks:frame ?mf; ks:argument ?ma; ks:role ?r.
             ?mf ks:denotes|ks:implies ?if. ?if a ks:Frame.
             ?ma ks:denotes ?ia.
             ?r ks:mappedTo ?p.
             BIND(rr:mint(fact:, ?m) AS ?g) }


    # COREFERENCE

    INSERT { GRAPH ?g { ?i1 owl:sameAs ?i2. } ?m ks:expresses ?g. }
    WHERE  { ?m a ks:CoreferenceMention; ks:coreferential ?m1, ?m2.
             ?m1 ks:denotes ?i1. ?m2 ks:denotes ?i2.
             FILTER(?m1 != ?m2) BIND(rr:mint(fact:, ?m) AS ?g) }

    INSERT { GRAPH ?g { ?i1 ks:include ?i2. } ?m ks:expresses ?g. }
    WHERE  { ?m a ks:CoreferenceMention; ks:coreferential ?m1; ks:coreferentialConjunct ?m2.
             ?m1 ks:denotes ?i1. ?m2 ks:denotes ?i2. BIND(rr:mint(fact:, ?m) AS ?g) }

    INSERT { GRAPH ?g { ?i3 owl:sameAs ?i2. } ?m1 ks:expresses ?g. }
    WHERE  { ?m1 a ks:FrameMention; ks:roleset ?rs1.
             ?m2 a ks:FrameMention; ks:denotes ?i2.
             ?m3 a ks:InstanceMention; ks:denotes ?i3.
             ?m12 a ks:ParticipationMention; ks:frame ?m1; ks:argument ?m2; ks:role ?r12.
             ?m13 a ks:ParticipationMention; ks:frame ?m1; ks:argument ?m3; ks:role ?r13.
             ?rs1 a ks:CopularVerb; ks:subjectRole ?r13; ks:complementRole ?r12.
             BIND(ks:mint(fact:, ?m1) AS ?g) }


    # POST-PROCESSING

    INSERT { GRAPH ?g { ?if ?p ?i } ?m ks:expresses ?g. }
    WHERE  { GRAPH ?g1 { ?if ?p ?ig. }
             GRAPH ?g2 { ?ig ks:include ?i. }
             { ?m ks:expresses ?g1. } UNION { ?m ks:expresses ?g2. }
             ?if a ks:Frame.
             BIND(ks:mint(?g1, ?g2) AS ?g) }
