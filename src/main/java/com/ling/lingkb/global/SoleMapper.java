package com.ling.lingkb.global;

import com.ling.lingkb.entity.LingDocument;
import com.ling.lingkb.entity.LingVector;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.InsertProvider;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * @author shipotian
 * @version 1.0.0
 * @since 2025/7/16
 */
@Mapper
public interface SoleMapper {

    @Select("select * from ling_vector where workspace=#{workspace} and is_persisted = 1")
    List<LingVector> queryPersistedVectors(String workspace);

    @Select("select * from ling_vector where workspace=#{workspace} and is_persisted = 0")
    List<LingVector> queryUnPersistedVectors(String workspace);

    @Update("update `ling_vector` set is_persisted = 1 where workspace=#{workspace} and is_persisted = 0")
    void updateUnPersistedVectors(String workspace);

    @InsertProvider(type = SqlWorkshop.class, method = "batchSaveVectors")
    void batchSaveVectors(List<LingVector> vectors);

    @InsertProvider(type = SqlWorkshop.class, method = "queryVectorTxtByNodeIds")
    List<String> queryVectorTxtByNodeIds(@Param("workspace") String workspace, @Param("nodeIds") List<Integer> nodeIds);

    @Insert("insert into `ling_document` (`file_id`, `workspace`, `text`, `author`, `source_file_name`, " +
            "`creation_date`, `page_count`, `char_count`, `word_count`, `sentence_count`, `keywords`) VALUES " +
            "(#{fileId}, #{workspace}, #{text}, #{author}, #{sourceFileName}, #{creationDate}, #{pageCount}, " +
            "#{charCount}, #{wordCount}, #{sentenceCount}, #{keywords})")
    void saveDocument(LingDocument document);

    @Select("select * from `ling_document` where file_id = #{fileId} limit 1")
    LingDocument queryDocumentByFileId(String fileId);

    class SqlWorkshop {
        public String batchSaveVectors(List<LingVector> vectors) {
            String content = vectors.stream().map(ve -> String
                    .format("('%s', '%s', '%s', %s)", ve.getWorkspace(), ve.getTxt(), ve.getVector(),
                            ve.isPersisted() ? 1 : 0)).collect(Collectors.joining(","));
            return "insert into `ling_vector` (`workspace`, `txt`, `vector`, `is_persisted`) values " + content;
        }

        public String queryVectorTxtByNodeIds(@Param("workspace") String workspace,
                                              @Param("nodeIds") List<Integer> nodeIds) {
            return String.format("select txt from `ling_vector` where workspace= '%s' and node_id in (%s)", workspace,
                    nodeIds.stream().map(Object::toString).collect(Collectors.joining(",")));
        }
    }
}
