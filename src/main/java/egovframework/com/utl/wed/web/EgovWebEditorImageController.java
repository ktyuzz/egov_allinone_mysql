package egovframework.com.utl.wed.web;

import java.util.List;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import egovframework.com.cmm.service.EgovProperties;
import egovframework.com.utl.fcc.service.EgovFileUploadUtil;
import egovframework.com.utl.fcc.service.EgovFormBasedFileUtil;
import egovframework.com.utl.fcc.service.EgovFormBasedFileVo;
import egovframework.rte.fdl.cryptography.EgovCryptoService;
import egovframework.com.cmm.EgovWebUtil;

import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * 웹에디터 이미지 upload 처리 Controller
 * @author 공통컴포넌트개발팀 한성곤
 * @since 2009.08.26
 * @version 1.0
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *
 *   수정일      수정자           수정내용
 *  -------    --------    ---------------------------
 *   2009.08.26  한성곤          최초 생성
 *   2017.08.31  장동한         path, physical 파라미터 노출 암호화 처리
 *   2017.12.12  장동한         출력 모듈 경로 변경 취약점 조치
 *
 * </pre>
 */
@Controller
public class EgovWebEditorImageController {
	
    /** 로그설정 */
	private static final Logger LOGGER = LoggerFactory.getLogger(EgovWebEditorImageController.class);

    /** 첨부파일 위치 지정 */
    private final String uploadDir = EgovProperties.getProperty("Globals.fileStorePath");

    /** 첨부 최대 파일 크기 지정 */
    private final long maxFileSize = 1024 * 1024 * 100;   //업로드 최대 사이즈 설정 (100M)
    
    /** 암호화서비스 */
	@Resource(name = "egovARIACryptoService")
	EgovCryptoService cryptoService;
	

    /**
     * 이미지 Upload 화면으로 이동한다.
     *
     * @return
     * @throws Exception
     */
    @RequestMapping(value="/utl/wed/insertImage.do", method=RequestMethod.GET)
    public String goInsertImage() throws Exception {

	return "egovframework/com/utl/wed/EgovInsertImage";
    }

    /**
     * 이미지 Upload를 처리한다.
     *
     * @param request
     * @param model
     * @return
     * @throws Exception
     */
    @RequestMapping(value="/utl/wed/insertImage.do", method=RequestMethod.POST)
    public String imageUpload(HttpServletRequest request, Model model) throws Exception {
	// Spring multipartResolver 미사용 시 (commons-fileupload 활용)
	//List<EgovFormBasedFileVo> list = EgovFormBasedFileUtil.uploadFiles(request, uploadDir, maxFileSize);

	// Spring multipartResolver 사용시
	List<EgovFormBasedFileVo> list = EgovFileUploadUtil.uploadFiles(request, uploadDir, maxFileSize);

	if (list.size() > 0) {
	    EgovFormBasedFileVo vo = list.get(0);	// 첫번째 이미지

	    String url = request.getContextPath()
	    + "/utl/web/imageSrc.do?"
	    + "path=" + this.encrypt(vo.getServerSubPath())
	    + "&physical=" + this.encrypt(vo.getPhysicalName())
	    + "&contentType=" + this.encrypt(vo.getContentType());

	    model.addAttribute("url", url);
	}

	return "egovframework/com/utl/wed/EgovInsertImage";
    }

    /**
     * 이미지 view를 제공한다.
     *
     * @param request
     * @param response
     * @throws Exception
     */
    @RequestMapping(value="/utl/web/imageSrc.do",method=RequestMethod.GET)
    public void download(HttpServletRequest request, HttpServletResponse response) throws Exception {
		String subPath = this.decrypt(request.getParameter("path"));
		//2017.12.12 - 출력 모듈 경로 변경 취약점 조치
		//저장파일경로 > 디텍토리 변경 체크 	
		subPath = EgovWebUtil.filePathBlackList(subPath);
		String physical = this.decrypt(request.getParameter("physical"));
		physical = EgovWebUtil.filePathBlackList(physical);
		String mimeType = this.decrypt(request.getParameter("contentType"));
		
		EgovFormBasedFileUtil.viewFile(response, uploadDir, subPath, physical, mimeType);
    }
    
    /**
     * 암호화
     *
     * @param encrypt
     */
    private String encrypt(String encrypt){
		try {
			return new String(new Base64().encode((byte[])cryptoService.encrypt(encrypt.getBytes("UTF-8"), "egovframe")));
        } catch(IllegalArgumentException e) {
    		LOGGER.error("[IllegalArgumentException] Try/Catch...usingParameters Runing : "+ e.getMessage());
        } catch (Exception e) {
        	LOGGER.error("[" + e.getClass() +"] :" + e.getMessage());
        }
		return encrypt;
    }
    
    /**
     * 복화화
     *
     * @param encrypt
     */
    private String decrypt(String decrypt){
		try {
			return new String((byte[])cryptoService.decrypt(new Base64().decode(decrypt.getBytes("UTF-8")), "egovframe"), "UTF-8");
		
        } catch(IllegalArgumentException e) {
    		LOGGER.error("[IllegalArgumentException] Try/Catch...usingParameters Runing : "+ e.getMessage());
        } catch (Exception e) {
        	LOGGER.error("[" + e.getClass() +"] :" + e.getMessage());
        }
		return decrypt;
    }
    
}
