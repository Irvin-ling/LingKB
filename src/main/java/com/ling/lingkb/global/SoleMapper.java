package com.ling.lingkb.global;

import com.ling.lingkb.entity.LingDocument;
import com.ling.lingkb.entity.LingDocumentLink;
import com.ling.lingkb.entity.LingVector;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.InsertProvider;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.SelectProvider;
import org.apache.ibatis.annotations.Update;

/**
 * @author shipotian
 * @version 1.0.0
 * @since 2025/7/16
 */
@Mapper
public interface SoleMapper {

    @Select("select * from ling_document_link where workspace=#{workspace} limit #{offset},1")
    LingDocumentLink queryLink(@Param("workspace") String workspace, @Param("offset") int offset);

    @Select("select desc_vector from ling_document_link where workspace=#{workspace}")
    List<LingDocumentLink> queryLinkVectors(String workspace);

    @Select("select * from ling_vector where workspace=#{workspace} and persisted = 1")
    List<LingVector> queryAllVector(String workspace);

    @Update("update ling_vector lv join (select id, -1 + (@rownum := @rownum + 1) as new_node_id " +
            "from ling_vector,(select @rownum := 0) r where workspace = #{workspace} order by id) tmp " +
            "on lv.id = tmp.id set lv.persisted = 1, lv.node_id = tmp.new_node_id")
    void resetVector(String workspace);

    @Select("select * from `ling_vector` where doc_id = #{docId} limit 1")
    LingVector queryVectorByDocId(String docId);

    @Select("select doc_id,node_id,txt,persisted from `ling_vector` where doc_id = #{docId}")
    List<LingVector> queryVectorsByDocId(String docId);

    @Delete("delete from `ling_vector` where node_id = #{nodeId}")
    void removeVectorByNodeId(int nodeId);

    @InsertProvider(type = SqlWorkshop.class, method = "batchSaveVectors")
    void batchSaveVectors(List<LingVector> vectors);

    @Insert({
            "<script>",
            "insert into `ling_document_link`",
            "(`doc_id`, `workspace`, `type`, `content`, `content_assistant`, `desc_text`, `desc_vector`)",
            "VALUES",
            "<foreach collection='list' item='link' separator=','>",
            "(#{link.docId}, #{link.workspace}, #{link.type}, ",
            "#{link.content}, #{link.contentAssistant}, #{link.descText}, #{link.descVector})",
            "</foreach>",
            "</script>"
    })
    void batchSaveLinks(List<LingDocumentLink> links);

    @SelectProvider(type = SqlWorkshop.class, method = "queryVectorTxtByNodeIds")
    List<String> queryVectorTxtByNodeIds(@Param("workspace") String workspace, @Param("nodeIds") List<Integer> nodeIds);

    @Insert("insert into `ling_document` (`doc_id`, `workspace`, `text`, `author`, `size`, `source_file_name`, " +
            "`creation_date`, `page_count`, `char_count`, `word_count`, `sentence_count`, `keywords`) VALUES " +
            "(#{docId}, #{workspace}, #{text}, #{author}, #{size}, #{sourceFileName}, #{creationDate}, #{pageCount}, " +
            "#{charCount}, #{wordCount}, #{sentenceCount}, #{keywords})")
    void saveDocument(LingDocument document);

    @Select("select * from `ling_document` where doc_id = #{docId} limit 1")
    LingDocument queryDocumentByDocId(String docId);

    @Select("select doc_id,workspace,author,size,source_file_name,creation_date,char_count,keywords " +
            "from `ling_document` where workspace=#{workspace}")
    List<LingDocument> queryDocument(String workspace);

    class SqlWorkshop {
        public String batchSaveVectors(List<LingVector> vectors) {
            String content = vectors.stream().map(ve -> String
                    .format("('%s', '%s', '%s', '%s', %s)", ve.getDocId(), ve.getWorkspace(), ve.getTxt(),
                            ve.getVector(), ve.isPersisted() ? 1 : 0)).collect(Collectors.joining(","));
            return "insert into `ling_vector` (`doc_id`, `workspace`, `txt`, `vector`, `persisted`) values " + content;
        }

        public String queryVectorTxtByNodeIds(@Param("workspace") String workspace,
                                              @Param("nodeIds") List<Integer> nodeIds) {
            return String.format("select txt from `ling_vector` where workspace= '%s' and node_id in (%s)", workspace,
                    nodeIds.stream().map(Object::toString).collect(Collectors.joining(",")));
        }
    }
}
